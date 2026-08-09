package com.moneytracker.app.ui.home;

import android.animation.ValueAnimator;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.moneytracker.app.MoneyTrackerApp;
import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.Account;
import com.moneytracker.app.data.entity.Category;
import com.moneytracker.app.data.relation.TransactionWithDetails;
import com.moneytracker.app.databinding.FragmentHomeBinding;
import com.moneytracker.app.ui.addtransaction.AddTransactionActivity;
import com.moneytracker.app.ui.alltransactions.AllTransactionsActivity;
import com.moneytracker.app.ui.savings.SavingsEnvelopesActivity;
import com.moneytracker.app.ui.wallet.AddEditAccountDialogFragment;
import com.moneytracker.app.ui.widget.OnTransactionInteractionListener;
import com.moneytracker.app.ui.widget.TransactionSelectionController;
import com.moneytracker.app.util.ElevationShadows;
import com.moneytracker.app.util.CurrencyFormatter;
import com.moneytracker.app.util.DisplayNameResolver;
import com.moneytracker.app.util.OnboardingPrefs;
import com.moneytracker.app.util.ProfileIcons;
import com.moneytracker.app.util.SettingsPrefs;
import com.moneytracker.app.util.ViewAnimations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    private static final String HIDDEN_BALANCE_TEXT = "••••••";

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private TransactionAdapter adapter;
    private AccountCardAdapter accountCardAdapter;
    private TransactionSelectionController selectionController;
    private List<TransactionWithDetails> latestTransactions = new ArrayList<>();

    private Double latestBalance;
    private double displayedBalance = Double.NaN;
    private ValueAnimator balanceAnimator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        ViewAnimations.fadeInContent(view);
        ElevationShadows.apply(view);

        selectionController = new TransactionSelectionController((AppCompatActivity) requireActivity(),
                MoneyTrackerApp.from(requireContext()).getTransactionRepository(),
                new TransactionSelectionController.Host() {
                    @Override
                    public TransactionWithDetails findTransactionById(long id) {
                        for (TransactionWithDetails t : latestTransactions) {
                            if (t.id == id) {
                                return t;
                            }
                        }
                        return null;
                    }

                    @Override
                    public void onSelectionChanged(Set<Long> selectedIds) {
                        if (adapter != null) {
                            adapter.setSelectedIds(selectedIds);
                        }
                    }
                });

        adapter = new TransactionAdapter(new OnTransactionInteractionListener() {
            @Override
            public void onTransactionLongClick(long transactionId) {
                selectionController.onItemLongClicked(transactionId);
            }

            @Override
            public void onTransactionClick(long transactionId) {
                selectionController.onItemClicked(transactionId);
            }
        });
        binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTransactions.setAdapter(adapter);

        accountCardAdapter = new AccountCardAdapter(item -> {
            if (Account.TYPE_SAVINGS.equals(item.account.getAccountType())) {
                startActivity(SavingsEnvelopesActivity.newIntent(requireContext(), item.account.getId()));
                return;
            }
            String resolvedName = DisplayNameResolver.resolve(requireContext(), item.account.getName());
            AddEditAccountDialogFragment.newInstanceForEdit(item.account, resolvedName)
                    .show(getChildFragmentManager(), "edit_account");
        });
        binding.recyclerAccountCards.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerAccountCards.setAdapter(accountCardAdapter);
        binding.btnAddAccountHome.setOnClickListener(v ->
                AddEditAccountDialogFragment.newInstanceForAdd().show(getChildFragmentManager(), "add_account"));

        binding.textGreeting.setText(resolveGreeting());

        binding.btnToggleBalance.setOnClickListener(v -> viewModel.toggleBalanceHidden());
        binding.btnSeeAll.setOnClickListener(v -> {
            List<Long> ids = viewModel.getResolvedAccountIds().getValue();
            startActivity(AllTransactionsActivity.newIntent(requireContext(),
                    ids != null ? ids : new ArrayList<>()));
        });

        binding.btnExpense.setOnClickListener(v -> openAddTransaction(Category.TYPE_EXPENSE));
        binding.btnIncome.setOnClickListener(v -> openAddTransaction(Category.TYPE_INCOME));

        binding.btnAvatar.setOnClickListener(v -> showProfileSwitcher());
        binding.btnBalanceAccounts.setOnClickListener(v ->
                BalanceAccountsPickerDialogFragment.newInstance().show(getChildFragmentManager(), "balance_accounts"));

        ViewAnimations.addPressFeedback(binding.btnExpense, binding.btnIncome, binding.btnSeeAll,
                binding.btnAvatar, binding.btnAddAccountHome);

        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-read every time this tab becomes visible, not just once at creation, since
        // the user may have just come back from toggling these in Settings.
        applyHomeSectionVisibility();
    }

    private void applyHomeSectionVisibility() {
        if (binding == null) {
            return;
        }
        binding.sectionBalance.setVisibility(
                SettingsPrefs.isHomeSectionVisible(requireContext(), SettingsPrefs.HomeSection.BALANCE)
                        ? View.VISIBLE : View.GONE);
        binding.sectionActions.setVisibility(
                SettingsPrefs.isHomeSectionVisible(requireContext(), SettingsPrefs.HomeSection.ACTIONS)
                        ? View.VISIBLE : View.GONE);
        binding.sectionAccounts.setVisibility(
                SettingsPrefs.isHomeSectionVisible(requireContext(), SettingsPrefs.HomeSection.ACCOUNTS)
                        ? View.VISIBLE : View.GONE);
        binding.sheetTransactions.setVisibility(
                SettingsPrefs.isHomeSectionVisible(requireContext(), SettingsPrefs.HomeSection.TRANSACTIONS)
                        ? View.VISIBLE : View.GONE);
    }

    private int resolveGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) {
            return R.string.greeting_morning;
        }
        if (hour >= 12 && hour < 18) {
            return R.string.greeting_afternoon;
        }
        return R.string.greeting_evening;
    }

    private void showProfileSwitcher() {
        ProfileSwitcherDialogFragment dialog = ProfileSwitcherDialogFragment.newInstance();
        dialog.setOnProfileChosenListener(profileId -> viewModel.switchProfile(profileId));
        dialog.show(getChildFragmentManager(), "profile_switcher");
    }

    private void observeViewModel() {
        viewModel.getActiveProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.btnAvatar.setImageResource(ProfileIcons.resolve(profile.getPictogram()));
                binding.textProfileName.setText(profile.getName());
            }
        });

        viewModel.getAccountsWithBalance().observe(getViewLifecycleOwner(), accounts -> {
            accountCardAdapter.submitList(accounts);
            boolean empty = accounts == null || accounts.isEmpty();
            binding.recyclerAccountCards.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getAccounts().observe(getViewLifecycleOwner(), accounts -> updateBalanceLabel());
        viewModel.getSelectedAccountIds().observe(getViewLifecycleOwner(), ids -> updateBalanceLabel());

        viewModel.getBalance().observe(getViewLifecycleOwner(), balance -> {
            latestBalance = balance;
            if (!Boolean.TRUE.equals(viewModel.getBalanceHidden().getValue())) {
                animateBalanceTo(balance);
            }
        });

        viewModel.getBalanceHidden().observe(getViewLifecycleOwner(), hidden -> {
            animateEyeIconSwap(Boolean.TRUE.equals(hidden));
            accountCardAdapter.setBalancesHidden(Boolean.TRUE.equals(hidden));
            if (Boolean.TRUE.equals(hidden)) {
                if (balanceAnimator != null) {
                    balanceAnimator.cancel();
                }
                displayedBalance = Double.NaN;
                ViewAnimations.animateTextMasking(binding.textBalance, HIDDEN_BALANCE_TEXT);
            } else {
                animateBalanceTo(latestBalance);
            }
        });

        viewModel.getRecentTransactions().observe(getViewLifecycleOwner(), transactions -> {
            latestTransactions = transactions != null ? transactions : new ArrayList<>();
            adapter.submitList(transactions);
            boolean empty = latestTransactions.isEmpty();
            binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerTransactions.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    private void updateBalanceLabel() {
        List<Account> accounts = viewModel.getAccounts().getValue();
        Set<Long> selection = viewModel.getSelectedAccountIds().getValue();
        if (accounts == null || binding == null) {
            return;
        }

        if (selection == null || selection.size() >= accounts.size()) {
            binding.textBalanceLabel.setText(R.string.balance_label_all);
            binding.rowAccountInfo.setVisibility(View.GONE);
            return;
        }

        List<String> names = new ArrayList<>();
        for (Account account : accounts) {
            if (selection.contains(account.getId())) {
                names.add(DisplayNameResolver.resolve(requireContext(), account.getName()));
            }
        }

        binding.textBalanceLabel.setText(
                names.size() == 1 ? R.string.balance_label_single : R.string.balance_label_multiple);
        binding.rowAccountInfo.setVisibility(View.VISIBLE);
        binding.textAccountInfo.setText(TextUtils.join(", ", names));
    }

    /** A single selected account shows in its own currency, unconverted; 2+ (including
     * "all") show in the preferred currency, since that's what the sum was converted into. */
    private String resolveBalanceCurrency() {
        List<Long> resolvedIds = viewModel.getResolvedAccountIds().getValue();
        List<Account> accounts = viewModel.getAccounts().getValue();
        if (resolvedIds != null && resolvedIds.size() == 1 && accounts != null) {
            long onlyId = resolvedIds.get(0);
            for (Account account : accounts) {
                if (account.getId() == onlyId) {
                    return account.getCurrencyCode();
                }
            }
        }
        return OnboardingPrefs.getPreferredCurrency(requireContext());
    }

    private void animateBalanceTo(Double balance) {
        if (balance == null || binding == null) {
            return;
        }
        String currency = resolveBalanceCurrency();

        double from = Double.isNaN(displayedBalance) ? 0 : displayedBalance;
        displayedBalance = balance;

        if (balanceAnimator != null) {
            balanceAnimator.cancel();
        }
        if (!SettingsPrefs.isAnimationsEnabled(requireContext())) {
            binding.textBalance.setText(CurrencyFormatter.format(balance.floatValue(), currency));
            return;
        }
        String finalCurrency = currency;
        balanceAnimator = ValueAnimator.ofFloat((float) from, balance.floatValue());
        balanceAnimator.setDuration(700);
        balanceAnimator.setInterpolator(new DecelerateInterpolator());
        balanceAnimator.addUpdateListener(animation ->
                binding.textBalance.setText(CurrencyFormatter.format((float) animation.getAnimatedValue(), finalCurrency)));
        balanceAnimator.start();
    }

    /** Animated strikethrough: the slash draws itself across the eye when hiding and
     * erases back out when showing, instead of a plain icon swap. */
    private void animateEyeIconSwap(boolean hidden) {
        if (!SettingsPrefs.isAnimationsEnabled(requireContext())) {
            binding.btnToggleBalance.setImageResource(hidden ? R.drawable.ic_eye_off : R.drawable.ic_eye);
            return;
        }
        binding.btnToggleBalance.setImageResource(hidden ? R.drawable.avd_eye_hide : R.drawable.avd_eye_show);
        Drawable icon = binding.btnToggleBalance.getDrawable();
        if (icon instanceof Animatable) {
            ((Animatable) icon).start();
        }
    }

    private void openAddTransaction(String type) {
        long accountId = viewModel.getQuickAddAccountId();
        startActivity(AddTransactionActivity.newIntent(requireContext(), type, accountId));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (balanceAnimator != null) {
            balanceAnimator.cancel();
            balanceAnimator = null;
        }
        binding = null;
    }
}
