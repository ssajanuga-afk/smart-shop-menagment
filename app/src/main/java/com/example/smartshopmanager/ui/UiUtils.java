package com.example.smartshopmanager.ui;

import android.app.Activity;
import android.os.Build;

/**
 * Shared UI compatibility helpers.
 */
public final class UiUtils {

    private UiUtils() {}

    /**
     * Suppresses open transition animation without using deprecated APIs on Android 14+.
     */
    @SuppressWarnings("deprecation")
    public static void disableOpenTransition(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0);
        } else {
            activity.overridePendingTransition(0, 0);
        }
    }
}
