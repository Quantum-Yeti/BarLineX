package me.theoria.barlinex.domain.barcode;

import com.google.zxing.BarcodeFormat;

public final class BarcodeValidator {

    private BarcodeValidator() {
        // Utility class
    }

    // Barcode generation validation

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

    // Scanned barcode check-digit validation

    public static String getCheckDigitStatus(
            String content,
            BarcodeFormat format
    ) {

        if (content == null || format == null) {
            return "Not applicable";
        }

        switch (format) {

            case EAN_13:
                return validateMod10(content, 13)
                        ? "Valid"
                        : "Invalid";

            case EAN_8:
                return validateMod10(content, 8)
                        ? "Valid"
                        : "Invalid";

            case UPC_A:
                return validateMod10(content, 12)
                        ? "Valid"
                        : "Invalid";

            case UPC_E:
                return isValidUpcECheckDigit(content)
                        ? "Valid"
                        : "Invalid";

            default:
                return "Not applicable";
        }
    }

    // EAN / UPC generation validation

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

    // Existing UPC-E generation validation

    private static String validateUpcE(String text) {

        // Preserve your existing generation behavior here.
        // This is separate from scanned UPC-E check-digit validation.
        return null;
    }

    // Scanned EAN / UPC-A check-digit validation

    private static boolean validateMod10(
            String content,
            int expectedLength
    ) {

        if (content == null
                || content.length() != expectedLength
                || !content.matches("[0-9]+")) {

            return false;
        }

        int sum = 0;
        int weight = 3;

        for (int i = content.length() - 2; i >= 0; i--) {

            int digit = content.charAt(i) - '0';

            sum += digit * weight;

            weight = (weight == 3) ? 1 : 3;
        }

        int expectedCheckDigit = (10 - (sum % 10)) % 10;

        int actualCheckDigit =
                content.charAt(content.length() - 1) - '0';

        return expectedCheckDigit == actualCheckDigit;
    }

    // Scanned UPC-E check-digit validation

    private static boolean isValidUpcECheckDigit(String content) {

        if (content == null
                || content.length() != 8
                || !content.matches("[0-9]+")) {

            return false;
        }

        String numberSystem = content.substring(0, 1);

        if (!numberSystem.equals("0")
                && !numberSystem.equals("1")) {

            return false;
        }

        String compressed = content.substring(1, 7);

        char lastDigit = compressed.charAt(5);

        String manufacturer;
        String product;

        switch (lastDigit) {

            case '0':
            case '1':
            case '2':

                manufacturer =
                        compressed.substring(0, 2) + lastDigit + "00";

                product =
                        "00" + compressed.substring(2, 5);

                break;

            case '3':

                manufacturer =
                        compressed.substring(0, 3) + "00";

                product =
                        "000" + compressed.substring(3, 5);

                break;

            case '4':

                manufacturer =
                        compressed.substring(0, 4) + "0";

                product =
                        "0000" + compressed.substring(4, 5);

                break;

            default:

                manufacturer =
                        compressed.substring(0, 5);

                product =
                        "0000" + lastDigit;

                break;
        }

        String expandedUpcA =
                numberSystem + manufacturer + product;

        return validateMod10(
                expandedUpcA + content.charAt(7),
                12
        );
    }
}
