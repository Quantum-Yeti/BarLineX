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

import me.theoria.barlinex.ui.adapter.BarcodeFormatAdapter;

public class BarcodeInputController {

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

    private final EditText inputContent;
    private final TextInputLayout contentInputLayout;
    private final MaterialAutoCompleteTextView formatDropdown;

    private BarcodeFormat selectedFormat = BarcodeFormat.QR_CODE;

    private OnFormatChangedListener listener;

    public interface OnFormatChangedListener {
        void onFormatChanged(BarcodeFormat format);
    }

    public BarcodeInputController(
            EditText inputContent,
            TextInputLayout contentInputLayout,
            MaterialAutoCompleteTextView formatDropdown,
            TextView textFormatCount
    ) {
        this.inputContent = inputContent;
        this.contentInputLayout = contentInputLayout;
        this.formatDropdown = formatDropdown;

        setupFormatDropdown(textFormatCount);
    }

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

                    selectedFormat = format;
                    formatDropdown.setText(getFormatDisplayName(format), false);

                    applyInputRules(format);

                    if (listener != null) {
                        listener.onFormatChanged(format);
                    }
                }
        );

        selectFormat(BarcodeFormat.QR_CODE);
    }

    public void setOnFormatChangedListener(
            OnFormatChangedListener listener
    ) {
        this.listener = listener;
    }

    public void selectFormat(BarcodeFormat format) {

        boolean supported = false;

        for (BarcodeFormat availableFormat : FORMATS) {
            if (availableFormat == format) {
                supported = true;
                break;
            }
        }

        if (!supported) {
            return;
        }

        selectedFormat = format;

        formatDropdown.setText(
                getFormatDisplayName(format),
                false
        );

        applyInputRules(format);
    }

    public BarcodeFormat getSelectedFormat() {
        return selectedFormat;
    }

    public String getCurrentContent() {
        return inputContent.getText().toString().trim();
    }

    public void setContent(String content) {
        inputContent.setText(content);
        inputContent.setSelection(inputContent.length());
    }

    public EditText getInputContent() {
        return inputContent;
    }

    public void setError(String message) {
        contentInputLayout.setError(message);
    }

    public void clearError() {
        contentInputLayout.setError(null);
    }

    private void applyInputRules(BarcodeFormat format) {

        inputContent.setInputType(
                isNumericOnly(format)
                        ? InputType.TYPE_CLASS_NUMBER
                        : InputType.TYPE_CLASS_TEXT
        );

        int maxLength = maxLengthFor(format);

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
        contentInputLayout.setHelperText(hintFor(format));
    }

    public String validate(String text) {
        return validate(selectedFormat, text);
    }

    public static String validate(
            BarcodeFormat format,
            String text
    ) {

        if (text == null || text.isEmpty()) {
            return "Enter some text first";
        }

        switch (format) {

            case CODE_39:
                return text.matches("[A-Z0-9 .$/+%-]+")
                        ? null
                        : "Use capitals, digits and - . $ / + % only";

            case EAN_13:
                return text.matches("\\d{12,13}")
                        ? checkDigitError(text, 13)
                        : "Needs 12 or 13 digits";

            case EAN_8:
                return text.matches("\\d{7,8}")
                        ? checkDigitError(text, 8)
                        : "Needs 7 or 8 digits";

            case UPC_A:
                return text.matches("\\d{11,12}")
                        ? checkDigitError(text, 12)
                        : "Needs 11 or 12 digits";

            case UPC_E:
                return text.matches("\\d{7,8}")
                        ? null
                        : "Needs 7 or 8 digits";

            case ITF:
                return text.matches("\\d+") && text.length() % 2 == 0
                        ? null
                        : "Needs an even number of digits";

            default:
                return null;
        }
    }

    private static String checkDigitError(
            String digits,
            int fullLength
    ) {

        if (digits.length() != fullLength) {
            return null;
        }

        int sum = 0;

        for (int i = 0; i < fullLength - 1; i++) {

            int digit = digits.charAt(fullLength - 2 - i) - '0';

            sum += digit * (i % 2 == 0 ? 3 : 1);
        }

        int expected = (10 - (sum % 10)) % 10;
        int actual = digits.charAt(fullLength - 1) - '0';

        return expected == actual
                ? null
                : "Check digit doesn't match";
    }

    private static String hintFor(BarcodeFormat format) {

        switch (format) {

            case CODE_128:
                return "Letters, numbers and symbols";

            case CODE_39:
                return "A–Z, 0–9 and - . $ / + %";

            case EAN_13:
                return "12 or 13 digits";

            case EAN_8:
            case UPC_E:
                return "7 or 8 digits";

            case UPC_A:
                return "11 or 12 digits";

            case ITF:
                return "Digits only, even count";

            default:
                return "Any text or link";
        }
    }

    private static int maxLengthFor(BarcodeFormat format) {

        switch (format) {

            case EAN_13:
                return 13;

            case EAN_8:
            case UPC_E:
                return 8;

            case UPC_A:
                return 12;

            default:
                return 0;
        }
    }

    private static boolean isNumericOnly(BarcodeFormat format) {

        switch (format) {

            case EAN_13:
            case EAN_8:
            case UPC_A:
            case UPC_E:
            case ITF:
                return true;

            default:
                return false;
        }
    }

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