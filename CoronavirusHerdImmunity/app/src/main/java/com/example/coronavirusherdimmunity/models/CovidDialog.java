package com.example.coronavirusherdimmunity.models;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.coronavirusherdimmunity.CovidApplication;
import com.example.coronavirusherdimmunity.R;

public class CovidDialog {

    /**
     * Create a dialog where notifies a "warning" message
     * @param context
     * @param title
     * @param msg
     */
    public static void warningDialog(Context context, String title, String msg){

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    /**
     * Create a dialog where notifies a "error" message (like internet disabled)
     * @param context
     * @param title
     * @param msg
     */
    public static void errorDialog(Context context, String title, String msg){

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    /**
     * Create a dialog where requires a "permission"
     * @param title
     * @param msg
     */
    public void permissionDialog(String title, String msg){

    }


    /**
     * Create a dialog where ask if you are sure to "validate" the action
     * @param title
     * @param msg
     */
    public void validationDialog(String title, String msg){

    }

    /**
     * @param activity: Activity context
     * @param id: patient id
     */
    public void showPatientIdDialog(Activity activity, String id){
        final Dialog dialog = new Dialog(activity);

        dialog.setContentView(R.layout.dialog_show_patient_id);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView text = (TextView) dialog.findViewById(R.id.tv_formatted_id);
        text.setText(id);

        Button button_cancel = (Button) dialog.findViewById(R.id.button_cancel);
        button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }

}
