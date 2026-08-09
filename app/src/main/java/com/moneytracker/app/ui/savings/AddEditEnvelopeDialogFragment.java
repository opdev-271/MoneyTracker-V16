package com.moneytracker.app.ui.savings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.moneytracker.app.R;
import com.moneytracker.app.data.entity.SavingsEnvelope;
import com.moneytracker.app.ui.widget.ColorSwatchRow;
import com.moneytracker.app.ui.widget.DialogButtonStyler;

/** Add/edit dialog for a single savings envelope; shares its ViewModel with the host Activity. */
public class AddEditEnvelopeDialogFragment extends DialogFragment {

    private static final String ARG_ID = "arg_id";
    private static final String ARG_NAME = "arg_name";
    private static final String ARG_GOAL = "arg_goal";
    private static final String ARG_CURRENT = "arg_current";
    private static final String ARG_COLOR = "arg_color";

    public static AddEditEnvelopeDialogFragment newInstanceForAdd() {
        AddEditEnvelopeDialogFragment fragment = new AddEditEnvelopeDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, 0);
        fragment.setArguments(args);
        return fragment;
    }

    public static AddEditEnvelopeDialogFragment newInstanceForEdit(SavingsEnvelope envelope) {
        AddEditEnvelopeDialogFragment fragment = new AddEditEnvelopeDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, envelope.getId());
        args.putString(ARG_NAME, envelope.getName());
        args.putDouble(ARG_GOAL, envelope.getGoalAmount());
        args.putDouble(ARG_CURRENT, envelope.getCurrentAmount());
        args.putInt(ARG_COLOR, envelope.getColor());
        fragment.setArguments(args);
        return fragment;
    }

    private EditText editName;
    private EditText editGoal;
    private ColorSwatchRow colorSwatchRow;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        SavingsEnvelopesActivity host = (SavingsEnvelopesActivity) requireActivity();
        SavingsEnvelopesViewModel viewModel = host.getViewModel();

        long envelopeId = requireArguments().getLong(ARG_ID, 0);
        boolean isEdit = envelopeId != 0;

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_envelope, null, false);
        editName = view.findViewById(R.id.edit_name);
        editGoal = view.findViewById(R.id.edit_goal);

        int existingColor = isEdit ? requireArguments().getInt(ARG_COLOR, 0) : 0;
        int initialColor = existingColor != 0 ? existingColor : ColorSwatchRow.DEFAULT_COLORS[1];
        LinearLayout colorRow = view.findViewById(R.id.color_row);
        colorSwatchRow = new ColorSwatchRow(requireContext(), colorRow, ColorSwatchRow.DEFAULT_COLORS, initialColor);

        if (isEdit) {
            editName.setText(requireArguments().getString(ARG_NAME));
            editGoal.setText(String.valueOf(requireArguments().getDouble(ARG_GOAL)));
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(),
                R.style.ThemeOverlay_MoneyTracker_Dialog)
                .setTitle(isEdit ? R.string.dialog_edit_envelope_title : R.string.dialog_add_envelope_title)
                .setView(view)
                .setPositiveButton(R.string.button_save, null)
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dismiss());

        if (isEdit) {
            builder.setNeutralButton(R.string.action_delete, (dialog, which) -> {
                SavingsEnvelope envelope = new SavingsEnvelope();
                envelope.setId(envelopeId);
                viewModel.deleteEnvelope(envelope);
                dismiss();
            });
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener((DialogInterface.OnShowListener) d -> {
            DialogButtonStyler.style(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (trySave(viewModel, envelopeId, isEdit)) {
                    dismiss();
                }
            });
        });
        return dialog;
    }

    private boolean trySave(SavingsEnvelopesViewModel viewModel, long envelopeId, boolean isEdit) {
        String name = editName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(requireContext(), R.string.error_envelope_name_required, Toast.LENGTH_SHORT).show();
            return false;
        }
        double goal;
        try {
            goal = Double.parseDouble(editGoal.getText().toString().trim());
        } catch (NumberFormatException e) {
            goal = 0;
        }

        SavingsEnvelope envelope = new SavingsEnvelope();
        envelope.setId(envelopeId);
        envelope.setName(name);
        envelope.setGoalAmount(goal);
        envelope.setCurrentAmount(isEdit ? requireArguments().getDouble(ARG_CURRENT) : 0);
        envelope.setColor(colorSwatchRow.getSelectedColor());
        envelope.setSortOrder(0);

        viewModel.saveEnvelope(envelope);
        return true;
    }
}
