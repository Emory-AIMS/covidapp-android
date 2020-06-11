package com.example.coronavirusherdimmunity.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.view.KeyEvent;

import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationManagerCompat;

import com.example.coronavirusherdimmunity.R;
import com.example.coronavirusherdimmunity.introduction.BluetoothActivity;
import com.example.coronavirusherdimmunity.introduction.LocationActivity;


public class PermissionRequest {

    private Context context;

    public PermissionRequest(Context packageContext) {
        this.context = packageContext;
    }

    /**
     * Check permissions if they are granted else go to introduction activities in order to enable them (Bluetooth, Location)
     *
     * @param: active_permission if 'true' the function checks permissions and enable them, else if 'false' it checks just permission
     * @return: 'true' if all permissions are granted, 'false' if at least one permission is not granted
     */
    public boolean checkPermissions(Boolean active_permission) {

        boolean ret_check_perm = true;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        } else if (!bluetoothAdapter.isEnabled()) { // if bluetooth is not enabled go to bluetooth activity in order to enable it

            if (active_permission) { // if 'active_permission' is true then requires to enable permission
                final Intent intent_bt = new Intent(context, BluetoothActivity.class);
                intent_bt.putExtra("permission_request", true); // notify next activity that permission is required
                showDialog(R.string.blue_disabled, R.string.blue_please_en, intent_bt);
            }

            ret_check_perm = false;

        }//if location is not enabled go to location activity in order to enable it
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                if (active_permission) { // if 'active_permission' is true then requires to enable permission
                    final Intent intent_loc = new Intent(context, LocationActivity.class);
                    intent_loc.putExtra("permission_request", true); // notify next activity that permission is required
                    showDialog(R.string.loc_disabled, R.string.loc_please_en, intent_loc);
                }

                ret_check_perm = false;
            }
        } else if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (active_permission) { // if 'active_permission' is true then requires to enable permission

                final Intent intent_loc = new Intent(context, LocationActivity.class);
                intent_loc.putExtra("permission_request", true); // notify next activity that permission is required

                // show alert dialog "Please, enable location"
                showDialog(R.string.loc_please_en, R.string.loc_please_en, intent_loc);
            }

            ret_check_perm = false;
        } else if (locationManager != null && !LocationManagerCompat.isLocationEnabled(locationManager)) {
            //TODO find a better communication message for this scenario
            // show alert dialog "Please, enable location"
            showDialog(R.string.loc_disabled, R.string.loc_please_en, new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            ret_check_perm = false;
        }

        return ret_check_perm;
    }

    private void showDialog(@StringRes int title, @StringRes int message, final Intent destination) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                context.startActivity(destination);
            }
        });
        builder.setCancelable(false);
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) { // if you click back button then close dialog
                    dialog.dismiss();
                    return true;
                }
                return false;
            }
        });

        if (context instanceof Activity && !((Activity) context).isFinishing() && !((Activity) context).isDestroyed())
            builder.show();
    }
}
