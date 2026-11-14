package me.theoria.barlinex.ui.common;

import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class WindowsInsetHelper {

    private WindowsInsetHelper() {
        // Prevent instantiation.
    }

    public static void applySystemBarInsets(View root) {

        if (root == null) return;

        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {

            int topInset = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
            ).top;

            view.setPadding(
                    initialLeft,
                    initialTop + topInset,
                    initialRight,
                    initialBottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }
}
