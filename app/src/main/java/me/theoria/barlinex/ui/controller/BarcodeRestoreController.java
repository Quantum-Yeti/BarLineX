package me.theoria.barlinex.ui.controller;

import android.content.Intent;
import android.widget.EditText;

import com.google.zxing.BarcodeFormat;

import me.theoria.barlinex.ui.main.MainViewModel;

public class BarcodeRestoreController {

    public static final String EXTRA_BARCODE_ID = "barcode_id";
    public static final String EXTRA_BARCODE_NAME = "barcode_name";
    public static final String EXTRA_BARCODE_CONTENT = "barcode_content";
    public static final String EXTRA_BARCODE_FORMAT = "barcode_format";
    public static final String EXTRA_BARCODE_NOTE = "barcode_note";
    public static final String EXTRA_BARCODE_CATEGORY = "barcode_category";

    private final MainViewModel mainViewModel;
    private final BarcodeInputController inputController;
    private final EditText barcodeNameInput;

    public BarcodeRestoreController(
            MainViewModel mainViewModel,
            BarcodeInputController inputController,
            EditText barcodeNameInput
    ) {
        this.mainViewModel = mainViewModel;
        this.inputController = inputController;
        this.barcodeNameInput = barcodeNameInput;
    }

    public boolean restoreBarcode(Intent intent) {

        if (intent == null ||
                !intent.hasExtra(EXTRA_BARCODE_CONTENT)) {
            return false;
        }

        String content = intent.getStringExtra(EXTRA_BARCODE_CONTENT);
        String name = intent.getStringExtra(EXTRA_BARCODE_NAME);
        String formatName = intent.getStringExtra(EXTRA_BARCODE_FORMAT);
        String note = intent.getStringExtra(EXTRA_BARCODE_NOTE);
        String category = intent.getStringExtra(EXTRA_BARCODE_CATEGORY);

        int barcodeId = intent.getIntExtra(EXTRA_BARCODE_ID, -1);

        BarcodeFormat format = parseBarcodeFormat(formatName);

        inputController.setPreviewUpdatesSuppressed(true);

        try {
            barcodeNameInput.setText(name != null ? name : "");

            mainViewModel.setBarcodeName(name);
            mainViewModel.setBarcodeNote(note);
            mainViewModel.setBarcodeCategory(category);

            mainViewModel.setEditingBarcodeId(
                    barcodeId >= 0 ? barcodeId : null
            );

            inputController.selectBarcodeFormat(format);
            inputController.setBarcodeContent(content);

        } finally {
            inputController.setPreviewUpdatesSuppressed(false);
        }

        inputController.refreshPreviewNow();

        intent.removeExtra(EXTRA_BARCODE_ID);
        intent.removeExtra(EXTRA_BARCODE_NAME);
        intent.removeExtra(EXTRA_BARCODE_CONTENT);
        intent.removeExtra(EXTRA_BARCODE_FORMAT);
        intent.removeExtra(EXTRA_BARCODE_NOTE);
        intent.removeExtra(EXTRA_BARCODE_CATEGORY);

        return true;
    }

    private BarcodeFormat parseBarcodeFormat(String formatName) {

        if (formatName == null || formatName.trim().isEmpty()) {
            return BarcodeFormat.QR_CODE;
        }

        try {
            BarcodeFormat format = BarcodeFormat.valueOf(formatName);

            return format;
        } catch (IllegalArgumentException exception) {
            return BarcodeFormat.QR_CODE;
        }
    }
}
