package me.theoria.barlinex.ui.formatter;

import com.google.zxing.BarcodeFormat;

public final class BarcodeFormatDisplay {

    private BarcodeFormatDisplay() {
        // Utility class
    }

    public static String getFormatDisplayName(BarcodeFormat format) {
        if (format == null) {
            return "Unknown";
        }

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