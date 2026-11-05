package me.theoria.barlinex.domain.rules;

import com.google.zxing.BarcodeFormat;

public final class BarcodeFormatRules {

    private BarcodeFormatRules() {
        // Utility class
    }

    public static int maxLengthFor(BarcodeFormat format) {

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

    public static boolean isNumericOnly(BarcodeFormat format) {

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

    public static String hintFor(BarcodeFormat format) {

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
}

