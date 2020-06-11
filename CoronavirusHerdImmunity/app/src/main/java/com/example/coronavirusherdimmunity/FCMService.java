package com.example.coronavirusherdimmunity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.coronavirusherdimmunity.notification.NotificationData;
import com.example.coronavirusherdimmunity.notification.channels.FirebaseNotificationChannel;
import com.example.coronavirusherdimmunity.resourceprovider.ResourceProviderDefault;
import com.example.coronavirusherdimmunity.utils.ApiManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    public static final String STATUS_UPDATE_RECEIVER_ACTION = "STATUS_UPDATE";
    private PreferenceManager preferenceManager;

    public FCMService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        preferenceManager = new PreferenceManager(getBaseContext());
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            Map<String, String> data = remoteMessage.getData();

            String _status = data.get("status");
            int status = 0;
            if (_status != null) {
                status = Integer.parseInt(_status);
            }
            preferenceManager.setPatientStatus(status);

            String _wlevel = data.get("warning_level");
            int wlevel = 0;
            if (_wlevel != null) {
                wlevel = Integer.parseInt(_wlevel);
            }
            preferenceManager.setWarningLevel(wlevel);

            String _filterId = data.get("filter_id");
            int filterId = 0;
            if (_filterId != null) {
                filterId = Integer.parseInt(_filterId);
            }
            preferenceManager.setAlertFilterId(filterId);

            final String link = data.get("link");
            preferenceManager.setAlertLink(link);

            final String language = data.get("language");
            String fb_language = null;
            try {
                String content = data.get("content");
                if (content != null) {
                    JSONObject jsonObject = new JSONObject(content);
                    fb_language = jsonObject.getString("language");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            preferenceManager.setAlertLanguage(language != null ? language : fb_language);

            if (link != null) {
                final int finalFilterId = filterId;
                Task.callInBackground(new Callable<JSONObject>() {
                    @Override
                    public JSONObject call() throws Exception {
                        return ApiManager.downloadAlert(link, finalFilterId, language);
                    }
                }).onSuccess(new Continuation<JSONObject, Object>() {
                    @Override
                    public Object then(Task<JSONObject> task) throws Exception {
                        preferenceManager.setAlertContent(task.getResult());
                        return null;
                    }
                });
            }

            String title = data.get("title");
            String message = data.get("message");

            sendNotification(getBaseContext(), title, message);

            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(new Intent(STATUS_UPDATE_RECEIVER_ACTION));
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(final String token) {
        Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                long deviceId = new PreferenceManager(getApplicationContext()).getDeviceId();
                ApiManager.registerPushToken(deviceId, token, new PreferenceManager(getApplicationContext()).getAuthToken());
                return null;
            }
        });
    }

    public static void sendNotification(Context context, String title, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            NotificationData notificationData = new NotificationData(null, title, message, pendingIntent);
            FirebaseNotificationChannel firebaseNotificationChannel = new FirebaseNotificationChannel(new ResourceProviderDefault(context), notificationManager);
            firebaseNotificationChannel.sendNotification(notificationData);
        }

    }
}