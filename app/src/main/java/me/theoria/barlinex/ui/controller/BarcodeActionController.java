package me.theoria.barlinex.ui.controller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;

import java.io.IOException;

import me.theoria.barlinex.R;
import me.theoria.barlinex.data.BarcodeRepository;
import me.theoria.barlinex.ui.BarcodePreviewActivity;
import me.theoria.barlinex.ui.helper.BarcodeImageHelper;
import me.theoria.barlinex.ui.helper.ClipboardHelper;
import me.theoria.barlinex.ui.viewmodel.BarcodeViewModel;

public class BarcodeActionController {

    // Intent extras
    public static final String EXTRA_BARCODE_CONTENT = "barcode_content";
    public static final String EXTRA_BARCODE_FORMAT = "barcode_format";

    // Views
    private final AppCompatActivity activity;
    private final MaterialButton buttonGenerate;
    private final Runnable onOpenHistory;

    // ViewModel
    private final BarcodeViewModel barcodeViewModel;

    public BarcodeActionController(
            AppCompatActivity activity,
            BarcodeViewModel barcodeViewModel,
            MaterialButton buttonGenerate,
            Runnable onOpenHistory
    ) {
        this.activity = activity;
        this.barcodeViewModel = barcodeViewModel;
        this.buttonGenerate = buttonGenerate;
        this.onOpenHistory = onOpenHistory;
    }

    // Save
    public void saveBarcode() {

        String content = getCurrentContent();
        BarcodeFormat format = getSelectedFormat();

        if (content.isEmpty()) {
            showToast("Enter some text first");
            return;
        }

        boolean generated = barcodeViewModel.generateBarcode(
                content,
                format,
                true,
                new BarcodeRepository.SaveCallback() {

                    @Override
                    public void onSaved(long id) {
                        showSaveSuccess();
                    }

                    @Override
                    public void onError(Exception error) {

                        Log.e(
                                "BarcodeActionController",
                                "Failed to save barcode",
                                error
                        );

                        Snackbar.make(
                                activity.findViewById(R.id.main),
                                "Barcode generated, but could not be saved",
                                Snackbar.LENGTH_LONG
                        ).setAnchorView(buttonGenerate).show();
                    }
                }
        );

        if (!generated) {
            return;
        }

        buttonGenerate.performHapticFeedback(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        ? HapticFeedbackConstants.CONFIRM
                        : HapticFeedbackConstants.VIRTUAL_KEY
        );
    }

    // Share
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

    // Copy
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

    // Export
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

            Log.e(
                    "BarcodeActionController",
                    "Failed to save barcode PNG",
                    e
            );

            showToast("Image save failed: " + e.getMessage());
        }
    }

    // Enlarged preview
    public void openEnlargedPreview() {

        String content = getCurrentContent();
        BarcodeFormat format = getSelectedFormat();

        if (!generateBarcode(content, format, false)) {
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

    // Barcode generation
    private boolean generateBarcode(
            String content,
            BarcodeFormat format,
            boolean saveToHistory
    ) {

        if (content.isEmpty()) {
            showToast("Enter some text first");
            return false;
        }

        return barcodeViewModel.generateBarcode(
                content,
                format,
                saveToHistory,
                null
        );
    }

    // State
    private String getCurrentContent() {
        String content = barcodeViewModel.getInputContent().getValue();
        return content != null ? content.trim() : "";
    }

    private BarcodeFormat getSelectedFormat() {
        BarcodeFormat format = barcodeViewModel.getSelectedFormat().getValue();
        return format != null ? format : BarcodeFormat.QR_CODE;
    }

    // Feedback
    private void showToast(String message) {
        Toast.makeText(
                activity,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    // Save feedback
    private void showSaveSuccess() {

        Snackbar.make(
                        activity.findViewById(R.id.main),
                        "Barcode saved",
                        Snackbar.LENGTH_LONG
                )
                .setAnchorView(buttonGenerate)
                .setAction("View", v -> onOpenHistory.run())
                .show();
    }
}