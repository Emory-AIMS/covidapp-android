package com.example.coronavirusherdimmunity;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.coronavirusherdimmunity.enums.Distance;
import com.example.coronavirusherdimmunity.utils.ApiManager;
import com.example.coronavirusherdimmunity.utils.BeaconDto;
import com.example.coronavirusherdimmunity.utils.StorageManager;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SyncDatabaseWorkManager extends Worker {
    public static final String TAG = "SyncDatabaseWorkManager";

    public SyncDatabaseWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        StorageManager storageManager = new StorageManager(getApplicationContext());
        final List<BeaconDto> groups = new ArrayList<>();

        final long nowInMilli = System.currentTimeMillis();
        Log.i(TAG, "PUSH new nowtime " + nowInMilli);
        long lastPushTimeInSeconds = preferenceManager.getLastInteractionPushTime();
        Date lastPushDate = new Date(lastPushTimeInSeconds * 1000);
        List<BeaconDto> beacons = storageManager.readBeacons(lastPushDate);
        if (beacons.size() == 0)
            return Result.success();

        Log.i(TAG, "PUSH start isPushing TRUE");
        boolean sendUserLocation = preferenceManager.getUserLocationPermission();

        ArrayList<Integer> dist = new ArrayList<>();
        ArrayList<Integer> rssi = new ArrayList<>();
        ArrayList<Double> distVal = new ArrayList<>();

        for (BeaconDto beacon : beacons) {
            if (groups.size() == 0) {
                groups.add(beacon);
                dist.clear();
                distVal.clear();
                rssi.clear();
                dist.add(beacon.distance.toInt());
                distVal.add(beacon.distanceValue);
                rssi.add(beacon.rssi);
            } else {
                BeaconDto lastGroup = groups.get(groups.size() - 1);
                if (lastGroup.identifier == beacon.identifier && lastGroup.timestmp + 3 * 60 > beacon.timestmp) {
                    groups.remove(lastGroup);

                    dist.add(beacon.distance.toInt());
                    distVal.add(beacon.distanceValue);
                    rssi.add(beacon.rssi);

                    Collections.sort(dist);
                    lastGroup.distance = Distance.valueOf(dist.get(dist.size() / 2));

                    Collections.sort(distVal);
                    lastGroup.distanceValue = distVal.get(distVal.size() / 2);

                    Collections.sort(rssi);
                    lastGroup.rssi = rssi.get(rssi.size() / 2);

                    lastGroup.interval = (int) Math.abs(lastGroup.timestmp - beacon.timestmp) + 10;

                    if (sendUserLocation) {
                        lastGroup.x = beacon.x;
                        lastGroup.y = beacon.y;
                    } else {
                        lastGroup.x = 0;
                        lastGroup.y = 0;
                    }
                    groups.add(lastGroup);
                } else {
                    groups.add(beacon);
                    dist.clear();
                    distVal.clear();
                    rssi.clear();
                    dist.add(beacon.distance.toInt());
                    distVal.add(beacon.distanceValue);
                    rssi.add(beacon.rssi);
                }
            }
        }

        final List<BeaconDto> reducedGroups = new ArrayList<>();
        for (BeaconDto b : groups) {
            if (reducedGroups.size() == 0) {
                reducedGroups.add(b);
            } else {
                BeaconDto lastGroup = groups.get(groups.size() - 1);
                if (lastGroup.distance == b.distance && b.timestmp < lastGroup.timestmp + lastGroup.interval + 30 * 1000) {
                    reducedGroups.remove(lastGroup);

                    lastGroup.rssi = (lastGroup.rssi + b.rssi) / 2;
                    lastGroup.interval = lastGroup.interval + b.interval;
                    lastGroup.distanceValue = (lastGroup.distanceValue + b.distanceValue) / 2;

                    reducedGroups.add(lastGroup);
                } else {
                    reducedGroups.add(b);
                }
            }
        }

        if (reducedGroups.size() > 0) {
            try {
                JSONObject result = ApiManager.pushInteractions(getApplicationContext(), reducedGroups, preferenceManager.getAuthToken());
                if (result != null) {
                    preferenceManager.setLastInteractionsPushTime(nowInMilli / 1000);
                    Log.i(TAG, "PUSH last time " + nowInMilli / 1000);
                    if (result.has("next_try")) {
                        int nextInMilli = result.getInt("next_try") * 1000;
                        CovidApplication.getInstance().setupWorkManager(nextInMilli, true);
                    }
                    if (result.has("location")) {
                        preferenceManager.setBackendLocation(result.getBoolean("location"));
                    }
                    if (result.has("distance_filter")) {
                        preferenceManager.setDistanceFilter(result.getDouble("distance_filter"));
                    } else {
                        preferenceManager.setDistanceFilter(-1);
                    }
                }
                Log.i(TAG, "PUSH end result " + result);
                return Result.success();
            } catch (Exception ex) {
                Log.i(TAG, "PUSH exception " + ex.getLocalizedMessage());
                ex.printStackTrace();
                String errorMessage = ex.getLocalizedMessage() != null ? ex.getLocalizedMessage() : ex.getMessage();
                FirebaseCrashlytics.getInstance().log("SyncDatabaseWorkManager error: " + errorMessage);
                return Result.failure();
            }
        }
        return Result.success();
    }
}
