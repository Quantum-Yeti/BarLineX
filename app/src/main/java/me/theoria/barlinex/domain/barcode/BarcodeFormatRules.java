package me.theoria.barlinex.domain.barcode;

import com.google.zxing.BarcodeFormat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BarcodeFormatRules {

    private static final List<BarcodeFormat> SUPPORTED_FORMATS =
            Collections.unmodifiableList(Arrays.asList(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.ITF
            ));

    private BarcodeFormatRules() {
        // Utility class
    }

    public static List<BarcodeFormat> getSupportedFormats() {
        return SUPPORTED_FORMATS;
    }

    public static boolean isSupported(BarcodeFormat format) {
        return format != null && SUPPORTED_FORMATS.contains(format);
    }

    public static int minLengthFor(BarcodeFormat format) {

        if (format == null) {
            return 0;
        }

        switch (format) {
            case EAN_13:
                return 12;

            case EAN_8:

            case UPC_E:
                return 7;

            case UPC_A:
                return 11;

            case ITF:
                return 2;

            default:
                return 0;
        }
    }

    public static int maxLengthFor(BarcodeFormat format) {

        if (format == null) {
            return 0;
        }

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

        if (format == null) {
            return false;
        }

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

    public static boolean requiresEvenLength(BarcodeFormat format) {
        return format == BarcodeFormat.ITF;
    }

    public static String hintFor(BarcodeFormat format) {

        if (format == null) {
            return "Select a barcode format";
        }

        switch (format) {
            case QR_CODE:
                return "Any text, link or URL";

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
                return "Unsupported barcode format";
        }
    }
}
