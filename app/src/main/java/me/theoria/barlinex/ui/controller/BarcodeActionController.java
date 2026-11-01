package me.theoria.barlinex.ui.controller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;

import java.io.IOException;

import me.theoria.barlinex.R;
import me.theoria.barlinex.ui.BarcodePreviewActivity;
import me.theoria.barlinex.ui.HistoryActivity;
import me.theoria.barlinex.ui.helper.BarcodeImageHelper;
import me.theoria.barlinex.ui.helper.ClipboardHelper;
import me.theoria.barlinex.ui.viewmodel.BarcodeViewModel;

public class BarcodeActionController {

    public static final String EXTRA_BARCODE_CONTENT = "barcode_content";
    public static final String EXTRA_BARCODE_FORMAT = "barcode_format";

    private final AppCompatActivity activity;
    private final BarcodeInputController inputController;
    private final BarcodeViewModel barcodeViewModel;
    private final MaterialButton buttonGenerate;

    public BarcodeActionController(
            AppCompatActivity activity,
            BarcodeInputController inputController,
            BarcodeViewModel barcodeViewModel,
            MaterialButton buttonGenerate
    ) {
        this.activity = activity;
        this.inputController = inputController;
        this.barcodeViewModel = barcodeViewModel;
        this.buttonGenerate = buttonGenerate;
    }

    public void saveBarcode() {

        String content = getCurrentContent();
        BarcodeFormat format = inputController.getSelectedFormat();

        String error = BarcodeInputController.validate(format, content);

        if (error != null) {
            inputController.setError(error);
            return;
        }

        inputController.clearError();

        barcodeViewModel.generateBarcode(content, format, true);

        buttonGenerate.performHapticFeedback(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        ? HapticFeedbackConstants.CONFIRM
                        : HapticFeedbackConstants.VIRTUAL_KEY
        );

        Snackbar.make(
                        activity.findViewById(R.id.main),
                        "Barcode saved",
                        Snackbar.LENGTH_LONG
                )
                .setAnchorView(buttonGenerate)
                .setAction("View", v -> openHistory())
                .show();
    }

    public void shareBarcode() {

        String content = getCurrentContent();

        if (content.isEmpty()) {
            showToast("Enter text to share first");
            return;
        }

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, content);

        activity.startActivity(
                Intent.createChooser(sendIntent, "Share barcode")
        );
    }

    public void copyBarcodeContent() {

        String content = getCurrentContent();

        if (content.isEmpty()) {
            showToast("Enter text to copy first");
            return;
        }

        boolean copied = ClipboardHelper.copyText(activity, content);

        showToast(
                copied
                        ? "Barcode content copied"
                        : "Clipboard is unavailable"
        );
    }

    public void saveBarcodeImage() {

        Bitmap bitmap = barcodeViewModel
                .getBarcodeImage()
                .getValue();

        if (bitmap == null) {
            showToast("No barcode image available. Generate one first.");
            return;
        }

        try {
            BarcodeImageHelper.saveImage(activity, bitmap);
            showToast("PNG saved to Pictures/BarLineX");

        } catch (IOException | SecurityException e) {

            android.util.Log.e(
                    "BarcodeActionController",
                    "Failed to save barcode PNG",
                    e
            );

            showToast("Image save failed: " + e.getMessage());
        }
    }

    public void openEnlargedPreview() {

        String content = getCurrentContent();
        BarcodeFormat format = inputController.getSelectedFormat();

        String error = BarcodeInputController.validate(format, content);

        if (error != null) {
            showToast("Enter a valid value to preview first");
            return;
        }

        Intent intent = new Intent(
                activity,
                BarcodePreviewActivity.class
        );

        intent.putExtra(EXTRA_BARCODE_CONTENT, content);
        intent.putExtra(EXTRA_BARCODE_FORMAT, format.name());

        activity.startActivity(intent);
    }

    private void openHistory() {

        activity.startActivity(
                new Intent(activity, HistoryActivity.class)
        );
    }

    private String getCurrentContent() {
        return inputController.getCurrentContent();
    }

    private void showToast(String message) {
        Toast.makeText(
                activity,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }
}
