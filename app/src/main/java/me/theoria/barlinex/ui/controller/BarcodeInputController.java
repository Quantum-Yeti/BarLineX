package me.theoria.barlinex.ui.controller;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;

import java.util.ArrayList;
import java.util.List;

import me.theoria.barlinex.domain.barcode.BarcodeFormatRules;
import me.theoria.barlinex.ui.common.BarcodeFormatAdapter;
import me.theoria.barlinex.ui.common.BarcodeFormatDisplay;
import me.theoria.barlinex.ui.main.MainViewModel;

public class BarcodeInputController {

    private static final long PREVIEW_DEBOUNCE_MS = 150L;

    // Views
    private final EditText barcodeContentInput;
    private final TextInputLayout barcodeContentInputLayout;
    private final MaterialAutoCompleteTextView barcodeFormatDropdown;
    private final View qrContentTypeSection;

    // ViewModel
    private final MainViewModel mainViewModel;

    // Preview
    private final Handler previewHandler = new Handler(Looper.getMainLooper());
    private Runnable previewRunnable;
    private BarcodePreviewController previewController;
    private boolean suppressPreviewUpdates = false;

    // Listener
    private OnBarcodeFormatChangedListener barcodeFormatChangedListener;

    public interface OnBarcodeFormatChangedListener {
        void onBarcodeFormatChanged(BarcodeFormat barcodeFormat);
    }

    public BarcodeInputController(
            MainViewModel mainViewModel,
            EditText barcodeContentInput,
            TextInputLayout barcodeContentInputLayout,
            MaterialAutoCompleteTextView barcodeFormatDropdown,
            TextView barcodeFormatCount,
            View qrContentTypeSection,
            MaterialAutoCompleteTextView qrContentTypeDropdown
    ) {
        this.mainViewModel = mainViewModel;
        this.barcodeContentInput = barcodeContentInput;
        this.barcodeContentInputLayout = barcodeContentInputLayout;
        this.barcodeFormatDropdown = barcodeFormatDropdown;
        this.qrContentTypeSection = qrContentTypeSection;

        setupBarcodeFormatDropdown(barcodeFormatCount);
    }

    // Text input listeners
    public void setupTextWatchers(
            EditText barcodeNameInput,
            BarcodePreviewController previewController
    ) {
        this.previewController = previewController;

        // Barcode name
        barcodeNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count
            ) {
                mainViewModel.setBarcodeName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        // Barcode content
        barcodeContentInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count
            ) {
                mainViewModel.setBarcodeContent(s.toString());

                if (suppressPreviewUpdates || BarcodeInputController.this.previewController == null) {
                    return;
                }

                BarcodeInputController.this.previewController.updatePreviewLabels();

                schedulePreviewUpdate();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void schedulePreviewUpdate() {
        if (previewController == null || suppressPreviewUpdates) {
            return;
        }

        cancelPendingPreviewUpdate();

        previewRunnable = () -> {
            if (previewController != null && !suppressPreviewUpdates) {
                previewController.updateLivePreview();
            }
        };

        previewHandler.postDelayed(
                previewRunnable,
                PREVIEW_DEBOUNCE_MS
        );
    }

    public void refreshPreviewNow() {
        cancelPendingPreviewUpdate();

        if (previewController != null && !suppressPreviewUpdates) {
            previewController.updateLivePreview();
        }
    }

    public void setPreviewUpdatesSuppressed(boolean suppressed) {
        suppressPreviewUpdates = suppressed;

        if (suppressed) {
            cancelPendingPreviewUpdate();
        }
    }

    private void cancelPendingPreviewUpdate() {
        if (previewRunnable != null) {
            previewHandler.removeCallbacks(previewRunnable);
            previewRunnable = null;
        }
    }

    public void cleanup() {
        cancelPendingPreviewUpdate();
        previewController = null;
    }

    // Format dropdown
    private void setupBarcodeFormatDropdown(TextView barcodeFormatCount) {

        List<BarcodeFormat> supportedBarcodeFormats =
                BarcodeFormatRules.getSupportedFormats();

        BarcodeFormatAdapter barcodeFormatAdapter =
                new BarcodeFormatAdapter(
                        barcodeContentInput.getContext(),
                        supportedBarcodeFormats
                );

        barcodeFormatDropdown.setAdapter(barcodeFormatAdapter);

        barcodeFormatCount.setText(
                supportedBarcodeFormats.size() + " formats"
        );

        barcodeFormatDropdown.setOnItemClickListener(
                (parent, view, position, id) -> {

                    BarcodeFormat barcodeFormat =
                            barcodeFormatAdapter.getItem(position);

                    if (barcodeFormat == null) {
                        return;
                    }

                    selectBarcodeFormat(barcodeFormat);

                    if (barcodeFormatChangedListener != null) {
                        barcodeFormatChangedListener
                                .onBarcodeFormatChanged(barcodeFormat);
                    }
                }
        );

        BarcodeFormat selectedBarcodeFormat =
                mainViewModel.getSelectedBarcodeFormat().getValue();

        if (BarcodeFormatRules.isSupported(selectedBarcodeFormat)) {
            selectBarcodeFormat(selectedBarcodeFormat);
        } else {
            selectBarcodeFormat(BarcodeFormat.QR_CODE);
        }
    }

    // Format selection
    public void setOnBarcodeFormatChangedListener(
            OnBarcodeFormatChangedListener listener
    ) {
        this.barcodeFormatChangedListener = listener;
    }

    public void selectBarcodeFormat(BarcodeFormat barcodeFormat) {

        if (!BarcodeFormatRules.isSupported(barcodeFormat)) {
            return;
        }

        mainViewModel.setSelectedBarcodeFormat(barcodeFormat);

        barcodeFormatDropdown.setText(
                BarcodeFormatDisplay.getFormatDisplayName(barcodeFormat),
                false
        );

        applyBarcodeInputRules(barcodeFormat);

        updateQrContentTypeVisibility(barcodeFormat);
    }

    // QR content visibility
    private void updateQrContentTypeVisibility(BarcodeFormat barcodeFormat) {

        qrContentTypeSection.setVisibility(
                barcodeFormat == BarcodeFormat.QR_CODE
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    // Barcode content
    public void setBarcodeContent(String barcodeContent) {

        String content = barcodeContent != null ? barcodeContent : "";

        mainViewModel.setBarcodeContent(content);

        if (!barcodeContentInput.getText().toString().equals(content)) {
            barcodeContentInput.setText(content);
            barcodeContentInput.setSelection(content.length());
        }
    }

    public EditText getBarcodeContentInput() {
        return barcodeContentInput;
    }

    // Input feedback
    public void setBarcodeInputError(String errorMessage) {
        barcodeContentInputLayout.setError(errorMessage);
    }

    public void clearBarcodeInputError() {
        barcodeContentInputLayout.setError(null);
    }

    // Input rules
    private void applyBarcodeInputRules(BarcodeFormat barcodeFormat) {

        boolean numericOnly =
                BarcodeFormatRules.isNumericOnly(barcodeFormat);

        barcodeContentInput.setInputType(
                numericOnly
                        ? InputType.TYPE_CLASS_NUMBER
                        : InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        barcodeContentInput.setSingleLine(false);
        barcodeContentInput.setHorizontallyScrolling(false);

        int maximumLength =
                BarcodeFormatRules.maxLengthFor(barcodeFormat);

        List<InputFilter> barcodeInputFilters = new ArrayList<>();

        if (maximumLength > 0) {
            barcodeInputFilters.add(
                    new InputFilter.LengthFilter(maximumLength)
            );
        }

        if (barcodeFormat == BarcodeFormat.CODE_39) {
            barcodeInputFilters.add(new InputFilter.AllCaps());
        }

        barcodeContentInput.setFilters(
                barcodeInputFilters.toArray(new InputFilter[0])
        );

        // Character counter
        barcodeContentInputLayout.setCounterEnabled(maximumLength > 0);

        if (maximumLength > 0) {
            barcodeContentInputLayout.setCounterMaxLength(maximumLength);
        }

        // Format-specific guidance
        // barcodeContentInputLayout.setHelperText(
        //         BarcodeFormatRules.hintFor(barcodeFormat)
        // );

        barcodeContentInputLayout.setError(null);
    }
}
