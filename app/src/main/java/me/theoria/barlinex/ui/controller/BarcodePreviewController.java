package me.theoria.barlinex.ui.controller;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.LifecycleOwner;

import com.google.zxing.BarcodeFormat;

import me.theoria.barlinex.ui.formatter.BarcodeFormatDisplay;
import me.theoria.barlinex.ui.viewmodel.BarcodeViewModel;

public class BarcodePreviewController {

    private final BarcodeViewModel barcodeViewModel;
    private final BarcodeInputController inputController;

    private final ImageView imagePreview;
    private final View previewPlaceholder;
    private final TextView textPlaceholder;

    private final TextView textPreviewFormat;
    private final TextView textPreviewContent;
    private final TextView textPreviewInfo;

    public BarcodePreviewController(
            BarcodeViewModel barcodeViewModel,
            BarcodeInputController inputController,
            ImageView imagePreview,
            View previewPlaceholder,
            TextView textPlaceholder,
            TextView textPreviewFormat,
            TextView textPreviewContent,
            TextView textPreviewInfo
    ) {
        this.barcodeViewModel = barcodeViewModel;
        this.inputController = inputController;

        this.imagePreview = imagePreview;
        this.previewPlaceholder = previewPlaceholder;
        this.textPlaceholder = textPlaceholder;

        this.textPreviewFormat = textPreviewFormat;
        this.textPreviewContent = textPreviewContent;
        this.textPreviewInfo = textPreviewInfo;
    }

    // Observers
    public void observeBarcode(LifecycleOwner lifecycleOwner) {

        barcodeViewModel.getBarcodeImage().observe(
                lifecycleOwner,
                bitmap -> {
                    if (bitmap == null) {
                        clearBarcodePreview();
                    } else {
                        displayBarcodePreview(bitmap);
                    }
                }
        );

        barcodeViewModel.getBarcodeStatus().observe(
                lifecycleOwner,
                message -> {
                    if (message == null || message.isEmpty()) {
                        inputController.clearBarcodeInputError();
                        return;
                    }

                    if (message.equals("Barcode saved to history!")
                            || message.equals("Generated, but failed to save.")) {
                        return;
                    }

                    inputController.setBarcodeInputError(message);
                }
        );
    }

    // Preview
    public void updateLivePreview() {

        updatePreviewLabels();

        String content = getCurrentContent();
        BarcodeFormat format = getSelectedFormat();

        if (content.isEmpty()) {
            inputController.clearBarcodeInputError();
            showPlaceholder("Your barcode appears here");
            return;
        }

        barcodeViewModel.generateBarcode(
                content,
                format,
                false
        );
    }

    public void updatePreviewLabels() {

        String content = getCurrentContent();
        BarcodeFormat format = getSelectedFormat();

        String formatLabel =
                BarcodeFormatDisplay.getFormatDisplayName(format);

        textPreviewFormat.setText(formatLabel.toUpperCase());

        textPreviewContent.setText(
                content.isEmpty() ? "Enter text to begin" : content
        );

        textPreviewInfo.setText(
                formatLabel
                        + " • "
                        + content.length()
                        + (content.length() == 1
                        ? " character"
                        : " characters")
        );
    }

    // State
    private String getCurrentContent() {
        String content = barcodeViewModel.getBarcodeContent().getValue();
        return content != null ? content.trim() : "";
    }

    private BarcodeFormat getSelectedFormat() {
        BarcodeFormat format = barcodeViewModel.getSelectedBarcodeFormat().getValue();
        return format != null ? format : BarcodeFormat.QR_CODE;
    }

    // Display
    private void displayBarcodePreview(Bitmap bitmap) {

        imagePreview.setImageBitmap(bitmap);

        if (imagePreview.getVisibility() != View.VISIBLE) {

            imagePreview.setAlpha(0f);
            imagePreview.setVisibility(View.VISIBLE);

            imagePreview.animate()
                    .alpha(1f)
                    .setDuration(120L)
                    .start();
        }

        previewPlaceholder.setVisibility(View.GONE);
    }

    private void clearBarcodePreview() {

        imagePreview.animate().cancel();
        imagePreview.setImageDrawable(null);
        imagePreview.setVisibility(View.GONE);

        previewPlaceholder.setVisibility(View.VISIBLE);
    }

    private void showPlaceholder(String message) {

        clearBarcodePreview();
        textPlaceholder.setText(message);
    }
}