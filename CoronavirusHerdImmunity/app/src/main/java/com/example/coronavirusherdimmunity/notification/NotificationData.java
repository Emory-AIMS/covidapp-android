package com.example.coronavirusherdimmunity.notification;

import android.app.PendingIntent;

public class NotificationData {
    private Integer id;
    private String title;
    private String message;
    private PendingIntent pendingIntent;

    public NotificationData(Integer id, String title, String message, PendingIntent pendingIntent) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.pendingIntent = pendingIntent;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }
}
