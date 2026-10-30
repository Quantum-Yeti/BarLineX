package me.theoria.barlinex.ui.helper;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.OutputStream;

public class BarcodeImageHelper {

    private static final String FOLDER_NAME = "Pictures/BarLineX";

    public static Uri saveImage(Context context, Bitmap bitmap)
            throws IOException {

        if (bitmap == null) {
            throw new IllegalArgumentException("Barcode image cannot be null.");
        }

        String fileName = "BarLineX_"
                + System.currentTimeMillis() + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    FOLDER_NAME
            );
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri imageUri = context.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );

        if (imageUri == null) {
            throw new IOException("Could not create the image file.");
        }

        try {
            try (OutputStream output = context.getContentResolver()
                    .openOutputStream(imageUri, "w")) {

                if (output == null) {
                    throw new IOException("Could not open the image file.");
                }

                boolean saved = bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        output
                );

                if (!saved) {
                    throw new IOException("PNG compression failed.");
                }

                output.flush();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues completed = new ContentValues();
                completed.put(MediaStore.Images.Media.IS_PENDING, 0);

                int updated = context.getContentResolver().update(
                        imageUri,
                        completed,
                        null,
                        null
                );

                if (updated == 0) {
                    throw new IOException("Could not finalize the image.");
                }
            }

            return imageUri;

        } catch (IOException | RuntimeException e) {
            context.getContentResolver().delete(imageUri, null, null);
            throw e;
        }
    }
}

