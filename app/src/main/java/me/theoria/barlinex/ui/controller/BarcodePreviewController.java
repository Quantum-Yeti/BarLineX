package me.theoria.barlinex.ui.controller;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.LifecycleOwner;

import com.google.zxing.BarcodeFormat;

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

        barcodeViewModel.getStatus().observe(
                lifecycleOwner,
                message -> {
                    if ("Enter some text first".equals(message)) {
                        inputController.setError(message);
                    }
                }
        );
    }

    public void updateLivePreview() {

        updatePreviewLabels();

        String content = inputController.getCurrentContent();
        BarcodeFormat format = inputController.getSelectedFormat();

        if (content.isEmpty()) {

            inputController.clearError();
            showPlaceholder("Your barcode appears here");
            return;
        }

        String error = inputController.validate(format, content);

        if (error != null) {

            inputController.setError(error);

            showPlaceholder(
                    "Waiting for a valid "
                            + BarcodeInputController.getFormatDisplayName(format)
                            + " value"
            );

            return;
        }

        inputController.clearError();

        barcodeViewModel.generateBarcode(
                content,
                format,
                false
        );
    }

    public void updatePreviewLabels() {

        String content = inputController.getCurrentContent();
        BarcodeFormat format = inputController.getSelectedFormat();

        String formatLabel =
                BarcodeInputController.getFormatDisplayName(format);

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