package me.theoria.barlinex.ui.controller;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;

import java.io.FileOutputStream;
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
import me.theoria.barlinex.ui.viewmodel.BarcodeViewModel;

public class BarcodeActionController {

    // Intent extras
    public static final String EXTRA_BARCODE_NAME = "barcode_name";
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

    // Share barcode image
    public void shareBarcodeImage() {

        Bitmap bitmap = barcodeViewModel
                .getBarcodeImage()
                .getValue();

        if (bitmap == null) {
            showToast("Generate a barcode first");
            return;
        }

        try {
            Uri imageUri = BarcodeImageHelper.createShareableImageUri(
                    activity,
                    bitmap
            );

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("image/png");
            sendIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            sendIntent.setClipData(
                    ClipData.newUri(
                            activity.getContentResolver(),
                            "Barcode Image",
                            imageUri
                    )
            );
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            activity.startActivity(
                    Intent.createChooser(sendIntent, "Share barcode image")
            );

        } catch (IOException | SecurityException e) {

            Log.e(
                    "BarcodeActionController",
                    "Failed to share barcode image",
                    e
            );

            showToast("Unable to share barcode image");
        }
    }

    // Copy barcode image
    public void copyBarcodeImage() {

        Bitmap bitmap = barcodeViewModel
                .getBarcodeImage()
                .getValue();

        if (bitmap == null) {
            showToast("Generate a barcode first");
            return;
        }

        try {
            Uri imageUri = BarcodeImageHelper.createShareableImageUri(
                    activity,
                    bitmap
            );

            ClipboardManager clipboardManager =
                    (ClipboardManager) activity.getSystemService(
                            AppCompatActivity.CLIPBOARD_SERVICE
                    );

            if (clipboardManager == null) {
                showToast("Clipboard is unavailable");
                return;
            }

            ClipData clipData = ClipData.newUri(
                    activity.getContentResolver(),
                    "Barcode Image",
                    imageUri
            );

            clipboardManager.setPrimaryClip(clipData);

            showToast("Barcode Image Copied");

        } catch (IOException | SecurityException e) {

            Log.e(
                    "BarcodeActionController",
                    "Failed to copy barcode image",
                    e
            );

            showToast("Unable to copy barcode image");
        }
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

    // Print barcode image
    public void printBarcodeImage() {

        Bitmap bitmap = barcodeViewModel
                .getBarcodeImage()
                .getValue();

        if (bitmap == null) {
            showToast("Generate a barcode first");
            return;
        }

        PrintManager printManager =
                (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);

        if (printManager == null) {
            showToast("Printing is unavailable");
            return;
        }

        String jobName = "BarLineX Barcode";

        printManager.print(
                jobName,
                new BarcodePrintAdapter(bitmap),
                new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .build()
        );
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

        intent.putExtra(
                EXTRA_BARCODE_NAME,
                barcodeViewModel.getBarcodeName().getValue()
        );

        intent.putExtra(EXTRA_BARCODE_CONTENT, content);
        intent.putExtra(EXTRA_BARCODE_FORMAT, format.name());

        activity.startActivity(intent);
    }

    // Customize barcode
    public void customizeBarcode() {
        showToast("Customization options coming soon.");
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

    // Printing
    private class BarcodePrintAdapter extends PrintDocumentAdapter {

        private final Bitmap bitmap;
        private PrintedPdfDocument pdfDocument;

        BarcodePrintAdapter(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public void onLayout(
                PrintAttributes oldAttributes,
                PrintAttributes newAttributes,
                CancellationSignal cancellationSignal,
                LayoutResultCallback callback,
                Bundle extras
        ) {

            if (cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }

            pdfDocument = new PrintedPdfDocument(
                    activity,
                    newAttributes
            );

            PrintDocumentInfo info = new PrintDocumentInfo.Builder(
                    "BarLineX_Barcode.pdf"
            )
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build();

            callback.onLayoutFinished(info, true);
        }

        @Override
        public void onWrite(
                PageRange[] pages,
                ParcelFileDescriptor destination,
                CancellationSignal cancellationSignal,
                PrintDocumentAdapter.WriteResultCallback callback
        ) {

            PdfDocument.Page page = pdfDocument.startPage(0);

            Canvas canvas = page.getCanvas();

            Rect contentRect = new Rect(
                    40,
                    40,
                    canvas.getWidth() - 40,
                    canvas.getHeight() - 40
            );

            Rect destinationRect = new Rect(
                    contentRect.left,
                    contentRect.top,
                    contentRect.right,
                    contentRect.bottom
            );

            canvas.drawBitmap(
                    bitmap,
                    null,
                    destinationRect,
                    null
            );

            pdfDocument.finishPage(page);

            try (FileOutputStream outputStream =
                         new FileOutputStream(destination.getFileDescriptor())) {

                pdfDocument.writeTo(outputStream);

                callback.onWriteFinished(
                        new PageRange[]{PageRange.ALL_PAGES}
                );

            } catch (IOException e) {
                callback.onWriteFailed(e.getMessage());

            } finally {
                pdfDocument.close();
                pdfDocument = null;
            }
        }
    }
}