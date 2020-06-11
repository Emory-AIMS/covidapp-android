package com.example.coronavirusherdimmunity.notification.channels;

import com.example.coronavirusherdimmunity.notification.NotificationChannelImportanceCompat;

public class ChannelInformation {
    private String id;
    private String name;
    private String description;
    private int defaultNotificationId;
    private NotificationChannelImportanceCompat channelImportanceCompat;

    public ChannelInformation(String id, String name, String description, int defaultNotificationId, NotificationChannelImportanceCompat channelImportanceCompat) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultNotificationId = defaultNotificationId;
        this.channelImportanceCompat = channelImportanceCompat;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultNotificationId() {
        return defaultNotificationId;
    }

    public NotificationChannelImportanceCompat getChannelImportanceCompat() {
        return channelImportanceCompat;
    }
}
