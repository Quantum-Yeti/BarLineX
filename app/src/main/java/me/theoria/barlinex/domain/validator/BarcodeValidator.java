
package me.theoria.barlinex.domain.validator;

import com.google.zxing.BarcodeFormat;

import me.theoria.barlinex.domain.rules.BarcodeFormatRules;

public final class BarcodeValidator {

    private BarcodeValidator() {
        // Utility class
    }

    public static String validate(BarcodeFormat format, String text) {

        if (!BarcodeFormatRules.isSupported(format)) {
            return "Unsupported barcode format";
        }

        if (text == null || text.trim().isEmpty()) {
            return "Enter some text first";
        }

        if (BarcodeFormatRules.isNumericOnly(format)
                && !text.matches("\\d+")) {
            return "Digits only";
        }

        int minLength = BarcodeFormatRules.minLengthFor(format);
        int maxLength = BarcodeFormatRules.maxLengthFor(format);

        if (minLength > 0 && text.length() < minLength) {
            return "Minimum " + minLength + " characters required";
        }

        if (maxLength > 0 && text.length() > maxLength) {
            return "Maximum " + maxLength + " characters allowed";
        }

        switch (format) {

            case CODE_39:
                return text.matches("[A-Z0-9 .$/+%-]+")
                        ? null
                        : "Use capitals, digits and - . $ / + % only";

            case EAN_13:
                return validateCheckDigitFormat(text, 13);

            case EAN_8:
                return validateCheckDigitFormat(text, 8);

            case UPC_A:
                return validateCheckDigitFormat(text, 12);

            case UPC_E:
                return validateUpcE(text);

            case ITF:
                return BarcodeFormatRules.requiresEvenLength(format)
                        && text.length() % 2 != 0
                        ? "Needs an even number of digits"
                        : null;

            default:
                return null;
        }
    }

    private static String validateCheckDigitFormat(
            String text,
            int fullLength
    ) {

        if (text.length() == fullLength
                && !hasValidCheckDigit(text)) {
            return "Check digit doesn't match";
        }

        return null;
    }

    private static boolean hasValidCheckDigit(String digits) {

        int sum = 0;

        for (int i = digits.length() - 2; i >= 0; i--) {

            int digit = digits.charAt(i) - '0';
            int positionFromRight = digits.length() - 2 - i;

            sum += digit * (positionFromRight % 2 == 0 ? 3 : 1);
        }

        int expected = (10 - (sum % 10)) % 10;
        int actual = digits.charAt(digits.length() - 1) - '0';

        return expected == actual;
    }

    private static String validateUpcE(String text) {

        //TODO: Full UPC-E number-system, expansion and checksum
        // validation still needs to be implemented.

        return null;
    }
}
