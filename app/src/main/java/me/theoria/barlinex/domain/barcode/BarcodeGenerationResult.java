package me.theoria.barlinex.domain.barcode;

import android.graphics.Bitmap;

public final class BarcodeGenerationResult {

    private final Bitmap bitmap;
    private final BarcodeStatistics statistics;

    public BarcodeGenerationResult(
            Bitmap bitmap,
            BarcodeStatistics statistics
    ) {
        this.bitmap = bitmap;
        this.statistics = statistics;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public BarcodeStatistics getStatistics() {
        return statistics;
    }
}