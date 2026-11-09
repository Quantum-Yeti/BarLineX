package me.theoria.barlinex.ui.controller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
    private final MaterialButton buttonSaveBarcode;
    private final Runnable onOpenHistory;

    // ViewModel
    private final BarcodeViewModel barcodeViewModel;

    private final ActivityResultLauncher<String> csvExportLauncher;

    private String pendingCsvContent;
    private BarcodeFormat pendingCsvFormat;

    public BarcodeActionController(
            AppCompatActivity activity,
            BarcodeViewModel barcodeViewModel,
            MaterialButton buttonSaveBarcode,
            Runnable onOpenHistory,
            ActivityResultLauncher<String> csvExportLauncher
    ) {
        this.activity = activity;
        this.barcodeViewModel = barcodeViewModel;
        this.buttonSaveBarcode = buttonSaveBarcode;
        this.onOpenHistory = onOpenHistory;
        this.csvExportLauncher = csvExportLauncher;
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
                        ).setAnchorView(buttonSaveBarcode).show();
                    }
                }
        );

        if (!generated) {
            return;
        }

        buttonSaveBarcode.performHapticFeedback(
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
                        ? "Barcode Content Copied"
                        : "Clipboard is unavailable"
        );
    }

    // Export
    public void exportBarcodeImage() {

        Bitmap bitmap = barcodeViewModel
                .getBarcodeImage()
                .getValue();

        if (bitmap == null) {
            showToast("No barcode image available. \nGenerate one first.");
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

    // Export menu
    public void showExportMenu() {
        String[] exportOptions = {"PNG Image", "CSV File"};

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Export Barcode")
                .setItems(exportOptions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            exportBarcodeImage();
                            break;
                        case 1:
                            exportBarcodeCsv();
                            break;
                    }
                })
                .show();
    }

    private void exportBarcodeCsv() {
        pendingCsvContent = getCurrentContent();
        pendingCsvFormat = getSelectedFormat();

        if (pendingCsvContent == null || pendingCsvContent.trim().isEmpty()) {
            Toast.makeText(activity, "Enter some text first", Toast.LENGTH_SHORT).show();
            return;
        }

        csvExportLauncher.launch("BarLineX_Barcode.csv");
    }

    public void writeBarcodeCsv(Uri uri) {
        if (pendingCsvContent == null || pendingCsvFormat == null) {
            return;
        }

        String exportedAt = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());

        String csvContent = "Content,Format,Exported At\n"
                + escapeCsv(pendingCsvContent) + ","
                + escapeCsv(pendingCsvFormat.name()) + ","
                + escapeCsv(exportedAt) + "\n";

        try (OutputStream outputStream =
                     activity.getContentResolver().openOutputStream(uri)) {

            if (outputStream == null) {
                throw new IOException("Unable to open output stream");
            }

            OutputStreamWriter writer = new OutputStreamWriter(
                    outputStream,
                    StandardCharsets.UTF_8
            );

            writer.write(csvContent);
            writer.flush();

            Toast.makeText(
                    activity,
                    "CSV exported successfully",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (IOException | SecurityException e) {
            Log.e("BarcodeActionController", "CSV export failed", e);

            Toast.makeText(
                    activity,
                    "Unable to export CSV",
                    Toast.LENGTH_SHORT
            ).show();
        } finally {
            pendingCsvContent = null;
            pendingCsvFormat = null;
        }
    }

    // Enlarged preview
    public void openEnlargedPreview() {

        String content = getCurrentContent();
        BarcodeFormat format = getSelectedFormat();

        if (!generateBarcode(content, format)) {
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
            BarcodeFormat format
    ) {

        if (content.isEmpty()) {
            showToast("Enter some text first");
            return false;
        }

        return barcodeViewModel.generateBarcode(
                content,
                format,
                false,
                null
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
                .setAnchorView(buttonSaveBarcode)
                .setAction("View", v -> onOpenHistory.run())
                .show();
    }

    // Helpers
    private String escapeCsv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}