package me.theoria.barlinex.ui.common;

import android.content.res.ColorStateList;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import me.theoria.barlinex.R;

public final class BottomNavigationHelper {

    private static final int ACTIVE_INDICATOR_COLOR = 0xFF30364F;

    private BottomNavigationHelper() {
        // Prevent instantiation.
    }

    public interface Listener {
        void onCreateSelected();
        void onDecodeSelected();
        void onHistorySelected();
    }

    public static void setup(
            BottomNavigationView navigationView,
            int selectedItemId,
            Listener listener
    ) {

        navigationView.setItemActiveIndicatorColor(
                ColorStateList.valueOf(ACTIVE_INDICATOR_COLOR)
        );

        navigationView.setSelectedItemId(selectedItemId);

        navigationView.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();

            if (itemId == R.id.nav_create) {
                listener.onCreateSelected();
                return true;
            }

            if (itemId == R.id.nav_decode) {
                listener.onDecodeSelected();
                return true;
            }

            if (itemId == R.id.nav_history) {
                listener.onHistorySelected();
                return true;
            }

            return false;
        });
    }
}