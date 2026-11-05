package me.theoria.barlinex.ui.controller;

import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.theoria.barlinex.domain.rules.BarcodeFormatRules;
import me.theoria.barlinex.ui.adapter.BarcodeFormatAdapter;
import me.theoria.barlinex.ui.viewmodel.BarcodeViewModel;

public class BarcodeInputController {

    // Supported formats
    private static final BarcodeFormat[] FORMATS = {
            BarcodeFormat.QR_CODE,
            BarcodeFormat.CODE_128
            // BarcodeFormat.CODE_39,
            // BarcodeFormat.EAN_13,
            // BarcodeFormat.EAN_8,
            // BarcodeFormat.UPC_A,
            // BarcodeFormat.UPC_E,
            // BarcodeFormat.ITF
    };

    // Views
    private final EditText inputContent;
    private final TextInputLayout contentInputLayout;
    private final MaterialAutoCompleteTextView formatDropdown;

    // ViewModel
    private final BarcodeViewModel viewModel;

    // Listener
    private OnFormatChangedListener listener;

    public interface OnFormatChangedListener {
        void onFormatChanged(BarcodeFormat format);
    }

    public BarcodeInputController(
            BarcodeViewModel viewModel,
            EditText inputContent,
            TextInputLayout contentInputLayout,
            MaterialAutoCompleteTextView formatDropdown,
            TextView textFormatCount
    ) {
        this.viewModel = viewModel;
        this.inputContent = inputContent;
        this.contentInputLayout = contentInputLayout;
        this.formatDropdown = formatDropdown;

        setupFormatDropdown(textFormatCount);
    }

    // Format dropdown

    private void setupFormatDropdown(TextView textFormatCount) {

        BarcodeFormatAdapter adapter = new BarcodeFormatAdapter(
                inputContent.getContext(),
                Arrays.asList(FORMATS)
        );

        formatDropdown.setAdapter(adapter);

        textFormatCount.setText(FORMATS.length + " formats");

        formatDropdown.setOnItemClickListener(
                (parent, view, position, id) -> {

                    BarcodeFormat format = adapter.getItem(position);

                    if (format == null) {
                        return;
                    }

                    selectFormat(format);

                    if (listener != null) {
                        listener.onFormatChanged(format);
                    }
                }
        );

        BarcodeFormat currentFormat = getSelectedFormat();

        if (isSupportedFormat(currentFormat)) {
            selectFormat(currentFormat);
        } else {
            selectFormat(BarcodeFormat.QR_CODE);
        }
    }

    // Format selection

    public void setOnFormatChangedListener(
            OnFormatChangedListener listener
    ) {
        this.listener = listener;
    }

    public void selectFormat(BarcodeFormat format) {

        if (!isSupportedFormat(format)) {
            return;
        }

        viewModel.setSelectedFormat(format);

        formatDropdown.setText(
                getFormatDisplayName(format),
                false
        );

        applyInputRules(format);
    }

    private boolean isSupportedFormat(BarcodeFormat format) {

        if (format == null) {
            return false;
        }

        for (BarcodeFormat availableFormat : FORMATS) {
            if (availableFormat == format) {
                return true;
            }
        }

        return false;
    }

    private BarcodeFormat getSelectedFormat() {
        BarcodeFormat format = viewModel.getSelectedFormat().getValue();

        return format != null ? format : BarcodeFormat.QR_CODE;
    }

    // Content

    public void setContent(String content) {

        String value = content != null ? content : "";

        viewModel.setInputContent(value);

        if (!inputContent.getText().toString().equals(value)) {
            inputContent.setText(value);
            inputContent.setSelection(value.length());
        }
    }

    public EditText getInputContent() {
        return inputContent;
    }

    // Input feedback

    public void setError(String message) {
        contentInputLayout.setError(message);
    }

    public void clearError() {
        contentInputLayout.setError(null);
    }

    // Input rules

    private void applyInputRules(BarcodeFormat format) {

        inputContent.setInputType(
                BarcodeFormatRules.isNumericOnly(format)
                        ? InputType.TYPE_CLASS_NUMBER
                        : InputType.TYPE_CLASS_TEXT
        );

        int maxLength = BarcodeFormatRules.maxLengthFor(format);

        List<InputFilter> filters = new ArrayList<>();

        if (maxLength > 0) {
            filters.add(new InputFilter.LengthFilter(maxLength));
        }

        if (format == BarcodeFormat.CODE_39) {
            filters.add(new InputFilter.AllCaps());
        }

        inputContent.setFilters(
                filters.toArray(new InputFilter[0])
        );

        contentInputLayout.setCounterEnabled(maxLength > 0);

        if (maxLength > 0) {
            contentInputLayout.setCounterMaxLength(maxLength);
        }

        contentInputLayout.setError(null);
        contentInputLayout.setHelperText(null);
    }

    // Format display

    public static String getFormatDisplayName(BarcodeFormat format) {

        switch (format) {

            case QR_CODE:
                return "QR Code";

            case CODE_128:
                return "Code 128";

            case CODE_39:
                return "Code 39";

            case EAN_13:
                return "EAN-13";

            case EAN_8:
                return "EAN-8";

            case UPC_A:
                return "UPC-A";

            case UPC_E:
                return "UPC-E";

            case ITF:
                return "ITF";

            default:
                return format.name();
        }
    }
}