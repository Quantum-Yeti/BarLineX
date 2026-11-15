package me.theoria.barlinex.domain.barcode;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.EnumMap;
import java.util.Map;

public final class BarcodeGenerator {

    private BarcodeGenerator() {
    }

    public static Bitmap generate(
            String content,
            BarcodeFormat format,
            int width,
            int height
    ) throws WriterException {

        return generateDetailed(
                content,
                format,
                width,
                height
        ).getBitmap();
    }

    public static BarcodeGenerationResult generateDetailed(
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

        if (format == null) {
            throw new IllegalArgumentException(
                    "Barcode format cannot be null."
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

        long generatedAt = System.currentTimeMillis();

        QRCode qrCode = null;
        int code128Modules = 0;

        BitMatrix matrix;

        if (format == BarcodeFormat.QR_CODE) {

            Map<EncodeHintType, Object> hints =
                    new EnumMap<>(EncodeHintType.class);

            hints.put(
                    EncodeHintType.CHARACTER_SET,
                    "UTF-8"
            );

            qrCode = Encoder.encode(
                    content,
                    ErrorCorrectionLevel.L,
                    hints
            );

            matrix = QRCodeWriter.renderResult(
                    qrCode,
                    width,
                    height,
                    2
            );

        } else {

            Map<EncodeHintType, Object> hints =
                    new EnumMap<>(EncodeHintType.class);

            hints.put(EncodeHintType.MARGIN, 2);

            matrix = new MultiFormatWriter().encode(
                    content,
                    format,
                    width,
                    height,
                    hints
            );

            code128Modules = new Code128Writer()
                    .encode(content)
                    .length;
        }

        Bitmap bitmap = Bitmap.createBitmap(
                matrix.getWidth(),
                matrix.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        for (int x = 0; x < matrix.getWidth(); x++) {
            for (int y = 0; y < matrix.getHeight(); y++) {

                bitmap.setPixel(
                        x,
                        y,
                        matrix.get(x, y)
                                ? Color.BLACK
                                : Color.WHITE
                );
            }
        }

        BarcodeStatistics statistics =
                BarcodeStatistics.analyze(
                        content,
                        format,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        qrCode,
                        code128Modules,
                        generatedAt
                );

        return new BarcodeGenerationResult(
                bitmap,
                statistics
        );
    }
}