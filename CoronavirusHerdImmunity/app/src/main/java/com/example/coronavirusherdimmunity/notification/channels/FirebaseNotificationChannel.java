package com.example.coronavirusherdimmunity.notification.channels;

import android.app.NotificationManager;

import com.example.coronavirusherdimmunity.R;
import com.example.coronavirusherdimmunity.resourceprovider.ResourceProvider;
import com.example.coronavirusherdimmunity.notification.NotificationChannelImportanceCompat;
import com.example.coronavirusherdimmunity.notification.NotificationsChannel;

public class FirebaseNotificationChannel extends NotificationsChannel {

    private ChannelInformation channelInformation;

    public FirebaseNotificationChannel(ResourceProvider resourceProvider, NotificationManager notificationManager) {
        super(notificationManager);
        channelInformation = new ChannelInformation(resourceProvider.getString(R.string.notification_channel_firebase_id),
                resourceProvider.getString(R.string.notification_channel_firebase_name),
                resourceProvider.getString(R.string.notification_channel_firebase_description),
                10000,
                NotificationChannelImportanceCompat.DEFAULT);
    }

    @Override
    public ChannelInformation getChannelInformation() {
        return channelInformation;
    }

}
