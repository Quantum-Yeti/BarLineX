package me.theoria.barlinex.ui.controller;

import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;

import java.util.ArrayList;
import java.util.List;

import me.theoria.barlinex.domain.rules.BarcodeFormatRules;
import me.theoria.barlinex.ui.adapter.BarcodeFormatAdapter;
import me.theoria.barlinex.ui.formatter.BarcodeFormatDisplay;
import me.theoria.barlinex.ui.viewmodel.BarcodeViewModel;

public class BarcodeInputController {

    // Views
    private final EditText barcodeContentInput;
    private final TextInputLayout barcodeContentInputLayout;
    private final MaterialAutoCompleteTextView barcodeFormatDropdown;
    private final View qrContentTypeSection;
    private final MaterialAutoCompleteTextView qrContentTypeDropdown;

    // ViewModel
    private final BarcodeViewModel barcodeViewModel;

    // Listener
    private OnBarcodeFormatChangedListener barcodeFormatChangedListener;

    public interface OnBarcodeFormatChangedListener {
        void onBarcodeFormatChanged(BarcodeFormat barcodeFormat);
    }

    public BarcodeInputController(
            BarcodeViewModel barcodeViewModel,
            EditText barcodeContentInput,
            TextInputLayout barcodeContentInputLayout,
            MaterialAutoCompleteTextView barcodeFormatDropdown,
            TextView barcodeFormatCount,
            View qrContentTypeSection,
            MaterialAutoCompleteTextView qrContentTypeDropdown
    ) {
        this.barcodeViewModel = barcodeViewModel;
        this.barcodeContentInput = barcodeContentInput;
        this.barcodeContentInputLayout = barcodeContentInputLayout;
        this.barcodeFormatDropdown = barcodeFormatDropdown;
        this.qrContentTypeSection = qrContentTypeSection;
        this.qrContentTypeDropdown = qrContentTypeDropdown;

        setupBarcodeFormatDropdown(barcodeFormatCount);
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
                barcodeViewModel.getSelectedBarcodeFormat().getValue();

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

        barcodeViewModel.setSelectedBarcodeFormat(barcodeFormat);

        barcodeFormatDropdown.setText(
                BarcodeFormatDisplay.getFormatDisplayName(barcodeFormat),
                false
        );

        applyBarcodeInputRules(barcodeFormat);

        updateQrContentTypeVisibility(barcodeFormat);
    }

    // Qr Selected
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

        barcodeViewModel.setBarcodeContent(content);

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
        /*barcodeContentInputLayout.setHelperText(
                BarcodeFormatRules.hintFor(barcodeFormat)
        );*/

        barcodeContentInputLayout.setError(null);
    }
}