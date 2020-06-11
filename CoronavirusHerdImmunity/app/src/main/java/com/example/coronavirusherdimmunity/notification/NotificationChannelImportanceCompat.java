package com.example.coronavirusherdimmunity.notification;

import android.app.NotificationManager;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

public enum NotificationChannelImportanceCompat {
    DEFAULT, MIN, MAX, LOW, HIGH;

    int getNotificationImportance() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            switch (this) {
                case DEFAULT:
                    return NotificationManager.IMPORTANCE_DEFAULT;

                case MAX:
                    return NotificationManager.IMPORTANCE_MAX;

                case MIN:
                    return NotificationManager.IMPORTANCE_MIN;

                case HIGH:
                    return NotificationManager.IMPORTANCE_HIGH;

                case LOW:
                    return NotificationManager.IMPORTANCE_LOW;

            }
        }
        return NotificationManagerCompat.IMPORTANCE_UNSPECIFIED;
    }
}