package me.theoria.barlinex.domain.barcode;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.MultiFormatWriter;

import java.util.EnumMap;
import java.util.Map;

public final class BarcodeGenerator {

    private BarcodeGenerator() {
        // Utility class; no instances needed.
    }

    public static Bitmap generate(
            String content,
            BarcodeFormat format,
            int width,
            int height
    ) throws WriterException {

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Content cannot be empty."
            );
        }

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Width and height must be positive."
            );
        }

        if (format != BarcodeFormat.QR_CODE
                && format != BarcodeFormat.CODE_128) {
            throw new IllegalArgumentException(
                    "Only QR_CODE and CODE_128 are supported."
            );
        }

        Map<EncodeHintType, Object> hints =
                new EnumMap<>(EncodeHintType.class);

        hints.put(EncodeHintType.MARGIN, 2);

        BitMatrix matrix = new MultiFormatWriter().encode(
                content,
                format,
                width,
                height,
                hints
        );

        Bitmap bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
        );

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(
                        x,
                        y,
                        matrix.get(x, y)
                                ? android.graphics.Color.BLACK
                                : android.graphics.Color.WHITE
                );
            }
        }

        return bitmap;
    }
}



