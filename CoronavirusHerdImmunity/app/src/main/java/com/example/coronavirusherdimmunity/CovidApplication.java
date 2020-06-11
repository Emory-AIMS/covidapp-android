package com.example.coronavirusherdimmunity;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.le.AdvertiseSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.coronavirusherdimmunity.enums.ApplicationStatus;
import com.example.coronavirusherdimmunity.enums.Distance;
import com.example.coronavirusherdimmunity.enums.PatientStatus;
import com.example.coronavirusherdimmunity.notification.NotificationData;
import com.example.coronavirusherdimmunity.notification.channels.PermissionNotificationChannel;
import com.example.coronavirusherdimmunity.resourceprovider.ResourceProviderDefault;
import com.example.coronavirusherdimmunity.utils.BeaconDto;
import com.example.coronavirusherdimmunity.utils.PermissionRequest;
import com.example.coronavirusherdimmunity.utils.StorageManager;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import bolts.Task;

public class CovidApplication extends Application implements BootstrapNotifier, BeaconConsumer, Configuration.Provider {

    private static final String BEACON_ID = "451720ea-5e62-11ea-bc55-0242ac130003";
    private static final String BEACON_ID_INTERMITTENT = "b1a75e04-5830-4ff8-956f-9ea8c555f462";

    private static final String TAG = "CovidApp";
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private MonitoringActivity monitoringActivity = null;
    private String cumulativeLog = "";

    private Beacon beaconIntermittent;
    private Beacon beaconSteady;
    private BeaconParser beaconParser;

    private BeaconManager beaconManager;
    private BeaconTransmitter beaconTransmitterIntermittent;
    private BeaconTransmitter beaconTransmitterSteady;

    private PatientStatus lastStatus = PatientStatus.NORMAL;      // Patient Status (NORMAL, INFECTED, QUARANTINE, HEALED, SUSPECT)
    private ApplicationStatus lastAppStatus = ApplicationStatus.ACTIVE; // App Status (ACTIVE if permissions are granted, INACTIVE if at least a permission is not granted)

    private static CovidApplication instance;
    private StorageManager storageManager;
    private PreferenceManager preferenceManager;
    private NotificationData notificationData;
    private PermissionNotificationChannel permissionNotificationChannel;

    public static CovidApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance;
    }

    public void onCreate() {
        instance = this;
        super.onCreate();

        storageManager = new StorageManager(getApplicationContext());
        preferenceManager = new PreferenceManager(getApplicationContext());
        lastStatus = preferenceManager.getPatientStatus();
        lastAppStatus = preferenceManager.getApplicationStatus();

        long deviceId = preferenceManager.getDeviceId();
        if (deviceId != -1) {
            initBeacon(deviceId);
        }

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                lastAppStatus = preferenceManager.getApplicationStatus();
                lastStatus = preferenceManager.getPatientStatus();
                updateNotification();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(FCMService.STATUS_UPDATE_RECEIVER_ACTION));
        setupWorkManager();
    }

    public void initBeacon(long deviceId) {
        beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);

        beaconIntermittent = new Beacon.Builder()
                .setId1(BEACON_ID_INTERMITTENT)
                .setId2("0")
                .setId3("0")
                .setManufacturer(0x004c)
                .setTxPower(-59)
                .setDataFields(new ArrayList<Long>())
                .build();

        beaconSteady = new Beacon.Builder()
                .setId1(BEACON_ID)
                .setId2(String.valueOf(deviceId % 65536)) // major
                .setId3(String.valueOf(deviceId / 65536)) // minor
                .setManufacturer(0x004c)
                .setTxPower(-59)
                .setDataFields(new ArrayList<Long>())
                .build();

        beaconParser = new BeaconParser()
//                .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");

        beaconTransmitterIntermittent = new BeaconTransmitter(getApplicationContext(), beaconParser);
        beaconTransmitterIntermittent.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        beaconTransmitterIntermittent.setAdvertiseTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

        beaconTransmitterSteady = new BeaconTransmitter(getApplicationContext(), beaconParser);
        beaconTransmitterSteady.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        beaconTransmitterSteady.setAdvertiseTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

        int transmissionSupportedStatus = BeaconTransmitter.checkTransmissionSupported(getApplicationContext());
        if (transmissionSupportedStatus == BeaconTransmitter.SUPPORTED ||
                transmissionSupportedStatus == BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS ||
                transmissionSupportedStatus == BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER) {
            beaconTransmitterSteady.startAdvertising(beaconSteady);
            beaconTransmitterIntermittent.startAdvertising(beaconIntermittent);
            mHandler.post(resetTransmission);
        } else {
            FirebaseCrashlytics.getInstance().recordException(new Throwable("This device don't support transmission. Status code " + transmissionSupportedStatus));
        }
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        BeaconManager.setDebug(BuildConfig.DEBUG);

        /**/
        lastAppStatus = preferenceManager.getApplicationStatus();
        lastStatus = preferenceManager.getPatientStatus();
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setContentTitle(
                String.format(getString(R.string.permanent_notification), lastAppStatus.toString(), lastStatus.toString()) /*, lastCount*/
        );
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("FOREGROUNDBEACON",
                    "Foreground beacon service", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Foreground beacon service");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            channel.setSound(null, null);

            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channel.getId());
        }
        beaconManager.enableForegroundServiceScanning(builder.build(), 456);
        beaconManager.setEnableScheduledScanJobs(false);
        beaconManager.setBackgroundBetweenScanPeriod(500);
        beaconManager.setBackgroundScanPeriod(600);
        /**/

        // wake up the app when a beacon is seen
        Region region = new Region("backgroundRegion",
                null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);
        backgroundPowerSaver = new BackgroundPowerSaver(this);

        // If you wish to test beacon detection in the Android Emulator, you can use code like this:
        // BeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
        // ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();

        beaconManager.bind(this);
    }

    private Handler mHandler = new Handler();
    private Runnable resetTransmission = new Runnable() {
        @Override
        public void run() {
            PermissionRequest permissions = new PermissionRequest(getApplicationContext());
            if (permissions.checkPermissions(false)) {  //if bluetooth and location are granted -> enable transmission

                if (lastAppStatus.toInt() == 1) {  //1: Inactive. Used to update just one time permanent notification when the permission is granted

                    preferenceManager.setApplicationStatus(0);
                    lastAppStatus = preferenceManager.getApplicationStatus();
                    lastStatus = preferenceManager.getPatientStatus();
                    updateNotification();
                }
                enableTransmission();
                cancelPermissionMissingNotification();

            } else {  //if bluetooth or location is not granted -> send a notification in order to alert the User

                PreferenceManager pm = preferenceManager;
                if (lastAppStatus.toInt() == 0 &&
                        !pm.isFirstTimeLaunch()) {  //0: Active and is not first time launch -> Used to send just one notification when the permission are not granted

                    preferenceManager.setApplicationStatus(1);
                    lastAppStatus = preferenceManager.getApplicationStatus();
                    lastStatus = preferenceManager.getPatientStatus();
                    updateNotification();  //update permanent notification

                    showPermissionMissingNotification();
                }
            }

            scheduleNextAlarm();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disableTransmission();
                }
            }, DateUtils.SECOND_IN_MILLIS * 10);
        }
    };

    public void disableMonitoring() {
        if (regionBootstrap != null) {
            regionBootstrap.disable();
            regionBootstrap = null;
        }
    }

    public void enableMonitoring() {
        Region region = new Region("backgroundRegion",
                null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);
    }

    public void disableTransmission() {
        Log.e(TAG, "Transmission stop");
        if (beaconTransmitterIntermittent != null) {
            beaconTransmitterIntermittent.stopAdvertising();
            beaconTransmitterIntermittent = null;
        }
    }

    public void enableTransmission() {
        Log.e(TAG, "Transmission start");
        if (beaconTransmitterIntermittent == null) {
            beaconTransmitterIntermittent = new BeaconTransmitter(getApplicationContext(), beaconParser);
            beaconTransmitterIntermittent.startAdvertising(beaconIntermittent);
        }
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.d(TAG, "FXX did enter region.");


        if (monitoringActivity != null) {
            // If the Monitoring Activity is visible, we log info about the beacons we have
            // seen on its display
            Log.d(TAG, "FXX I see a beacon again");
            logToDisplay("I see a beacon again");
        } else {
            // If the monitoring activity is not in the foreground, we send a notification to the user.
            Log.d(TAG, "FXXSending notification.");
            if (BuildConfig.DEBUG) {
                sendNotification();
            }
        }
    }

    @Override
    public void didExitRegion(Region region) {
        logToDisplay("I no longer see a beacon.");
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        logToDisplay("Current region state is: " + (state == 1 ? "INSIDE" : "OUTSIDE (" + state + ")"));
    }

    private void sendNotification() {
/*        Notification.Builder builder =
                new Notification.Builder(this)
                        .setContentTitle("Beacon Reference Application")
                        .setContentText("An beacon is nearby.")
                        .setSmallIcon(R.mipmap.ic_launcher);*/
/*

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this, MainActivity.class));
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("BEACONNOTIFICATION", "BEACON NOTIFICATION", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Beacon notification");
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channel.getId());
        }
        notificationManager.notify(1, builder.build());
*/

    }

    public void setMonitoringActivity(MonitoringActivity activity) {
        this.monitoringActivity = activity;
    }

    private void logToDisplay(String line) {
        cumulativeLog += (line + "\n");
        if (this.monitoringActivity != null) {
            this.monitoringActivity.updateLog(cumulativeLog);
        }
    }

    public String getLog() {
        return cumulativeLog;
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                Log.d(TAG, "didRangeBeaconsInRegion called with beacon count:  " + beacons.size());
                final List<BeaconDto> beaconDtos = new ArrayList<>();
                double x = 0;
                double y = 0;
                for (Beacon beacon : beacons) {
                    if (beacon.getId1().toString().equals(BEACON_ID)) {

                        // id2 major - id3 minor
                        long deviceId = 65536 * beacon.getId3().toInt() + beacon.getId2().toInt();

                        Distance distance = Distance.FAR;
                        if (beacon.getDistance() <= 0.4) {
                            distance = Distance.IMMEDIATE;
                        } else if (beacon.getDistance() <= 2) {
                            distance = Distance.NEAR;
                        }

                        double distanceFilter = preferenceManager.getDistanceFilter();
                        if (distanceFilter < 0 || beacon.getDistance() <= distanceFilter) {
                            //boolean sendBackendLocation = preferenceManager.getBackendLocation();
                            boolean sendUserLocation = preferenceManager.getUserLocationPermission();

                            //if (sendBackendLocation && sendUserLocation) {
                            if (sendUserLocation && x == 0 && y == 0) {
                                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                                if (locationManager != null &&
                                        (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                                    Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                                    x = (location == null ? 0 : location.getLatitude());
                                    y = (location == null ? 0 : location.getLongitude());
                                }
                            }
                            BeaconDto beaconDto = new BeaconDto(deviceId, beacon.getRssi(), distance, beacon.getDistance(), x, y);
                            beaconDtos.add(beaconDto);
                        }
                    }
                }

                Task.callInBackground(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        if (beaconDtos.size() > 0)
                            storageManager.insertBeacon(beaconDtos);

                        if (lastStatus != preferenceManager.getPatientStatus()) {
                            lastStatus = preferenceManager.getPatientStatus();
                            lastAppStatus = preferenceManager.getApplicationStatus();
                            updateNotification();
                        }
                        return null;
                    }
                });
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
    }

    private void scheduleNextAlarm() {
        AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, intent, 0);

        long nextVerification = getNextVerificationTimestamp();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextVerification, pendingIntent);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, nextVerification, pendingIntent);
        }
    }

    private long getNextVerificationTimestamp() {
        Calendar nextVerification = Calendar.getInstance();
        int currentMinute = nextVerification.get(Calendar.MINUTE);
        int currentHour = nextVerification.get(Calendar.HOUR_OF_DAY);
        int nextHour = currentHour;
        int nextMinute;
        if (currentMinute == 59) {
            if (currentHour == 23) {
                nextHour = 0;
            } else {
                nextHour = currentHour + 1;
            }
            nextMinute = 0;
        } else {
            nextMinute = currentMinute + 1;
        }
        nextVerification.set(Calendar.HOUR_OF_DAY, nextHour);
        nextVerification.set(Calendar.MINUTE, nextMinute);
        nextVerification.set(Calendar.SECOND, 0);
        return nextVerification.getTimeInMillis();
    }

    private void updateNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setPriority(Notification.PRIORITY_MIN);
        builder.setContentTitle(
                String.format(getString(R.string.permanent_notification), lastAppStatus, lastStatus.toString()) /*, lastCount)*/
        );

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel("FOREGROUNDBEACON",
//                    "Foreground beacon service", NotificationManager.IMPORTANCE_MIN);
//            channel.setDescription("Foreground beacon service");
//            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
//            channel.enableLights(false);
//            channel.enableVibration(false);
//            channel.setShowBadge(false);
//            channel.setSound(null, null);
//
//
//            notificationManager.createNotificationChannel(channel);
            builder.setChannelId("FOREGROUNDBEACON");
        }
        notificationManager.notify(456, builder.build());
    }

    private void setupWorkManager() {
        setupWorkManager(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, false);
    }

    private void showPermissionMissingNotification() {

        NotificationManager notificationManager =
                (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            String title = getString(R.string.notification_appstatus_title);
            String msg = getString(R.string.notification_appstatus_msg);

            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0 /* Request code */, intent,
                    PendingIntent.FLAG_ONE_SHOT);

            notificationData = new NotificationData(10, title, msg, pendingIntent);
            permissionNotificationChannel = new PermissionNotificationChannel(new ResourceProviderDefault(getContext()), notificationManager);
            permissionNotificationChannel.sendNotification(notificationData);
        }

    }

    private void cancelPermissionMissingNotification() {
        if (notificationData != null && permissionNotificationChannel != null) {
            permissionNotificationChannel.cancelNotification(notificationData.getId());
        }
    }

    public void setupWorkManager(long intervalInMilliseconds, boolean shouldReplace) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest saveRequest =
                new PeriodicWorkRequest.Builder(SyncDatabaseWorkManager.class, intervalInMilliseconds, TimeUnit.MILLISECONDS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniquePeriodicWork(
                        SyncDatabaseWorkManager.TAG,
                        shouldReplace ? ExistingPeriodicWorkPolicy.REPLACE : ExistingPeriodicWorkPolicy.KEEP,
                        saveRequest
                );
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .build();
    }

    public static class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            CovidApplication.instance.mHandler.post(CovidApplication.instance.resetTransmission);
        }
    }
}
