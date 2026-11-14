package me.theoria.barlinex.ui.common;

import android.os.SystemClock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public final class SplashScreenHelper {

    private static final long SPLASH_DURATION_MS = 2000L;

    private SplashScreenHelper() {
        // Utility class
    }

    public static void install(AppCompatActivity activity) {

        SplashScreen splashScreen =
                SplashScreen.installSplashScreen(activity);

        long startTime = SystemClock.elapsedRealtime();

        splashScreen.setKeepOnScreenCondition(() ->
                SystemClock.elapsedRealtime() - startTime
                        < SPLASH_DURATION_MS
        );
    }
}