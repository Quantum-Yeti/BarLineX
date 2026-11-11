package me.theoria.barlinex.ui.helper;

import com.google.zxing.BarcodeFormat;

public class DecodeResult {

    private final String content;
    private final BarcodeFormat format;
    private final long scannedAt;

    public DecodeResult(
            String content,
            BarcodeFormat format
    ) {
        this.content = content;
        this.format = format;
        this.scannedAt = System.currentTimeMillis();
    }

    public String getContent() {
        return content;
    }

    public BarcodeFormat getFormat() {
        return format;
    }

    public long getScannedAt() {
        return scannedAt;
    }

    public int getCharacterCount() {
        return content.codePointCount(
                0,
                content.length()
        );
    }

    public boolean isTwoDimensional() {
        return format == BarcodeFormat.QR_CODE;
    }
}