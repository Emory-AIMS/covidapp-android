package com.example.coronavirusherdimmunity.notification.channels;

import android.app.NotificationManager;

import com.example.coronavirusherdimmunity.R;
import com.example.coronavirusherdimmunity.notification.NotificationChannelImportanceCompat;
import com.example.coronavirusherdimmunity.notification.NotificationsChannel;
import com.example.coronavirusherdimmunity.resourceprovider.ResourceProvider;

public class PermissionNotificationChannel extends NotificationsChannel {

    private ChannelInformation channelInformation;

    public PermissionNotificationChannel(ResourceProvider resourceProvider, NotificationManager notificationManager) {
        super(notificationManager);
        channelInformation = new ChannelInformation(resourceProvider.getString(R.string.notification_channel_firebase_id),
                resourceProvider.getString(R.string.notification_channel_firebase_name),
                resourceProvider.getString(R.string.notification_channel_firebase_description),
                10001,
                NotificationChannelImportanceCompat.DEFAULT);
    }

    @Override
    public ChannelInformation getChannelInformation() {
        return channelInformation;
    }

}
