package com.example.coronavirusherdimmunity.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.coronavirusherdimmunity.CovidApplication;
import com.example.coronavirusherdimmunity.R;
import com.example.coronavirusherdimmunity.notification.channels.ChannelInformation;

public abstract class NotificationsChannel {
    private NotificationManager notificationManager;

    public abstract ChannelInformation getChannelInformation();

    public NotificationsChannel(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ChannelInformation channelInformation = getChannelInformation();
            String name = channelInformation.getName();
            String descriptionText = channelInformation.getDescription();
            int importance = channelInformation.getChannelImportanceCompat().getNotificationImportance();
            NotificationChannel channel = new NotificationChannel(channelInformation.getId(), name, importance);
            channel.setDescription(descriptionText);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void sendNotification(int notificationId, Notification notification) {
        createNotificationChannel();
        notificationManager.notify(notificationId, notification);
    }

    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }

    public void sendNotification(NotificationData data) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(CovidApplication.getContext(), getChannelInformation().getId())
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(data.getTitle())
                        .setContentText(data.getMessage())
                        .setAutoCancel(true)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(data.getMessage())
                        )
                        .setSound(defaultSoundUri)
                        .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})       //Vibration
                        .setContentIntent(data.getPendingIntent());

        if (data.getId() != null)
            sendNotification(data.getId(), notificationBuilder.build());
        else
            sendNotification(getChannelInformation().getDefaultNotificationId(), notificationBuilder.build());
    }

}
