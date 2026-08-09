package com.moneytracker.app.ui.savings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.SavingsEnvelope;
import com.moneytracker.app.databinding.ActivitySavingsEnvelopesBinding;
import com.moneytracker.app.util.ElevationShadows;
import com.moneytracker.app.util.CurrencyFormatter;
import com.moneytracker.app.util.DisplayNameResolver;
import com.moneytracker.app.util.LocaleHelper;
import com.moneytracker.app.util.ViewAnimations;

public class SavingsEnvelopesActivity extends AppCompatActivity {

    private static final String EXTRA_ACCOUNT_ID = "extra_account_id";

    public static Intent newIntent(Context context, long accountId) {
        Intent intent = new Intent(context, SavingsEnvelopesActivity.class);
        intent.putExtra(EXTRA_ACCOUNT_ID, accountId);
        return intent;
    }

    private ActivitySavingsEnvelopesBinding binding;
    private SavingsEnvelopesViewModel viewModel;
    private SavingsEnvelopeAdapter adapter;
    private String currencyCode = "CZK";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyStoredLocale(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_up_in, R.anim.stay);
        binding = ActivitySavingsEnvelopesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ElevationShadows.apply(binding.getRoot());

        long accountId = getIntent().getLongExtra(EXTRA_ACCOUNT_ID, 0);
        viewModel = new ViewModelProvider(this, new SavingsEnvelopesViewModel.Factory(getApplication(), accountId))
                .get(SavingsEnvelopesViewModel.class);

        adapter = new SavingsEnvelopeAdapter(new SavingsEnvelopeAdapter.OnEnvelopeInteractionListener() {
            @Override
            public void onEnvelopeClick(SavingsEnvelope envelope) {
                DepositEnvelopeDialogFragment.newInstance(envelope.getId(), envelope.getName())
                        .show(getSupportFragmentManager(), "deposit_envelope");
            }

            @Override
            public void onEnvelopeEditClick(SavingsEnvelope envelope) {
                AddEditEnvelopeDialogFragment.newInstanceForEdit(envelope)
                        .show(getSupportFragmentManager(), "edit_envelope");
            }
        }, currencyCode);
        binding.recyclerEnvelopes.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerEnvelopes.setAdapter(adapter);

        binding.btnClose.setOnClickListener(v -> closeWithSlideDown());
        binding.btnAddEnvelope.setOnClickListener(v ->
                AddEditEnvelopeDialogFragment.newInstanceForAdd().show(getSupportFragmentManager(), "add_envelope"));
        ViewAnimations.addPressFeedback(binding.btnAddEnvelope);

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getAccount().observe(this, account -> {
            if (account == null) {
                return;
            }
            currencyCode = account.getCurrencyCode();
            adapter.setCurrencyCode(currencyCode);
            binding.textTitle.setText(DisplayNameResolver.resolve(this, account.getName()));
            updateUnallocated();
        });

        viewModel.getBalance().observe(this, balance -> {
            binding.textTotalBalance.setText(CurrencyFormatter.format(balance != null ? balance : 0, currencyCode));
            updateUnallocated();
        });

        viewModel.getTotalEnveloped().observe(this, total -> {
            binding.textEnvelopedTotal.setText(CurrencyFormatter.format(total != null ? total : 0, currencyCode));
            updateUnallocated();
        });

        viewModel.getEnvelopes().observe(this, envelopes -> {
            adapter.submitList(envelopes);
            boolean empty = envelopes == null || envelopes.isEmpty();
            binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.recyclerEnvelopes.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    private void updateUnallocated() {
        Double balance = viewModel.getBalance().getValue();
        Double enveloped = viewModel.getTotalEnveloped().getValue();
        double unallocated = (balance != null ? balance : 0) - (enveloped != null ? enveloped : 0);
        binding.textUnallocated.setText(CurrencyFormatter.format(unallocated, currencyCode));
    }

    public SavingsEnvelopesViewModel getViewModel() {
        return viewModel;
    }

    private void closeWithSlideDown() {
        finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.stay, R.anim.slide_down_out);
    }
}
