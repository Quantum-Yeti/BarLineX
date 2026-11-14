package me.theoria.barlinex.ui.common;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class ClipboardHelper {

    public static boolean copyText(Context context, String text) {

        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(
                        Context.CLIPBOARD_SERVICE
                );

        if (clipboard == null) {
            return false;
        }

        ClipData clip = ClipData.newPlainText(
                "Barcode Content",
                text
        );

        clipboard.setPrimaryClip(clip);

        return true;
    }
}

