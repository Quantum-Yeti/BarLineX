package me.theoria.barlinex.domain.validator;

import com.google.zxing.BarcodeFormat;

public final class BarcodeValidator {

    private BarcodeValidator() {
        // Utility class
    }

    public static String validate(BarcodeFormat format, String text) {

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

    private static String checkDigitError(String digits, int fullLength) {

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
}

