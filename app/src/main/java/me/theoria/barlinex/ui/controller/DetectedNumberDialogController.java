package me.theoria.barlinex.ui.controller;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class DetectedNumberDialogController {

    public interface Listener {
        void onNumberConfirmed(String number);
    }

    private final AppCompatActivity activity;

    public DetectedNumberDialogController(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void show(String detectedNumber, Listener listener) {

        EditText numberInput = new EditText(activity);

        numberInput.setSingleLine(true);
        numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        numberInput.setText(detectedNumber);
        numberInput.setSelection(numberInput.getText().length());
        numberInput.setHint("Review or correct the number");

        int horizontalPadding = (int) (
                24 * activity.getResources()
                        .getDisplayMetrics().density
        );

        numberInput.setPadding(
                horizontalPadding,
                numberInput.getPaddingTop(),
                horizontalPadding,
                numberInput.getPaddingBottom()
        );

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Review detected number")
                .setMessage(
                        "Check the digits before using them in your barcode."
                )
                .setView(numberInput)
                .setNegativeButton("Retake", null)
                .setPositiveButton("Use Number", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String confirmedNumber = numberInput
                                .getText()
                                .toString()
                                .trim();

                        if (confirmedNumber.isEmpty()) {
                            numberInput.setError("Enter a number first.");
                            return;
                        }

                        if (listener != null) {
                            listener.onNumberConfirmed(confirmedNumber);
                        }

                        dialog.dismiss();
                    });
        });

        dialog.show();
    }
}
