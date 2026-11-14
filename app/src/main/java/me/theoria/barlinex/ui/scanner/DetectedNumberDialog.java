package me.theoria.barlinex.ui.scanner;

import android.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class DetectedNumberDialog {

    public interface Listener {

        void onTextConfirmed(String text);

        default void onDialogDismissed() {
            // Optional dismissal callback.
        }
    }

    private final AppCompatActivity activity;

    public DetectedNumberDialog(
            AppCompatActivity activity
    ) {
        this.activity = activity;
    }

    public void show(String detectedText, Listener listener) {

        EditText textInput = new EditText(activity);

        textInput.setSingleLine(false);
        textInput.setMinLines(3);
        textInput.setMaxLines(6);
        textInput.setGravity(Gravity.TOP | Gravity.START);

        textInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );

        textInput.setText(detectedText);
        textInput.setSelection(textInput.getText().length());
        textInput.setHint("Review or correct the detected text");

        int horizontalPadding = (int) (
                24 * activity.getResources()
                        .getDisplayMetrics().density
        );

        textInput.setPadding(
                horizontalPadding,
                textInput.getPaddingTop(),
                horizontalPadding,
                textInput.getPaddingBottom()
        );

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Review detected text")
                .setMessage(
                        "Check the recognized text before using it in your barcode."
                )
                .setView(textInput)
                .setNegativeButton("Retake", null)
                .setPositiveButton("Use Text", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String confirmedText = textInput
                                .getText()
                                .toString()
                                .trim();

                        if (confirmedText.isEmpty()) {
                            textInput.setError("Enter some text first.");
                            return;
                        }

                        if (listener != null) {
                            listener.onTextConfirmed(confirmedText);
                        }

                        dialog.dismiss();
                    });
        });

        dialog.setOnDismissListener(dialogInterface -> {
            if (listener != null) {
                listener.onDialogDismissed();
            }
        });

        dialog.show();
    }
}