package com.example.coronavirusherdimmunity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.coronavirusherdimmunity.enums.PatientStatus;
import com.example.coronavirusherdimmunity.models.CovidDialog;
import com.example.coronavirusherdimmunity.models.TouchListener;
import com.example.coronavirusherdimmunity.utils.ApiManager;
import com.example.coronavirusherdimmunity.utils.PermissionRequest;
import com.example.coronavirusherdimmunity.utils.QRCodeGenerator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.robertsimoes.shareable.Shareable;

import java.util.concurrent.Callable;

import bolts.Continuation;

import static com.example.coronavirusherdimmunity.R.id;
import static com.example.coronavirusherdimmunity.R.layout;
import static com.example.coronavirusherdimmunity.R.string;
import static com.example.coronavirusherdimmunity.utils.CheckSum.computeChecksum;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Context mContext;
    PreferenceManager preferenceManager;
    final String MESSAGE = "Help us. Together we can save lives. https://coronavirus-outbreak-control.github.io/web/index.html";
    final String LABEL = "Coronavirus Outbreak Control Link";
    final String URL = "https://coronavirus-outbreak-control.github.io/web/index.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);

        mContext = this;

        preferenceManager = new PreferenceManager(this);
        preferenceManager.setFirstTimeLaunch(false);

        // De-comment to test different statuses
        //preferenceManager.setPatientStatus(0);
        this.writeQRCode();

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("COVAPP", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        final String token = task.getResult().getToken();

                        bolts.Task.callInBackground(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                long deviceId = new PreferenceManager(mContext).getDeviceId();
                                ApiManager.registerPushToken(deviceId, token, new PreferenceManager(mContext).getAuthToken());
                                return null;
                            }
                        }).onSuccess(new Continuation<Object, Object>() {
                            @Override
                            public Object then(bolts.Task<Object> task) throws Exception {
                                return null;
                            }
                        });
                    }
                });


        if (BuildConfig.DEBUG) {
            findViewById(id.openMonitoring).setVisibility(View.VISIBLE);
            findViewById(id.openMonitoring).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent myIntent = new Intent(mContext, MonitoringActivity.class);
                    startActivity(myIntent);
                }
            });
        } else {
            findViewById(id.openMonitoring).setVisibility(View.GONE);
        }


        final Handler handler=new Handler();
        handler.post(new Runnable(){
            @Override
            public void run() {
                writePatientInfo();
                //writeInteractions();
                handler.postDelayed(this,500); // set time here to refresh textView
            }
        });


        PermissionRequest permissions = new PermissionRequest(MainActivity.this);
        permissions.checkPermissions(true); //check if bluetooth and location are enabled else go to activity in order to enable them


        /* BUTTONS */

        findViewById(id.button_show_id).setOnClickListener(this);
        findViewById(id.button_more_info).setOnClickListener(this);
        findViewById(R.id.how_it_works).setOnClickListener(this);
        findViewById(R.id.facebook).setOnClickListener(this);
        findViewById(R.id.twitter).setOnClickListener(this);
        findViewById(R.id.linkedin).setOnClickListener(this);
        findViewById(R.id.messenger).setOnClickListener(this);
        findViewById(R.id.whatsapp).setOnClickListener(this);
        findViewById(R.id.sms).setOnClickListener(this);
        findViewById(R.id.mail).setOnClickListener(this);
        findViewById(R.id.other).setOnClickListener(this);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //writeInteractions();
            writePatientInfo();
        }
    };

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(FCMService.STATUS_UPDATE_RECEIVER_ACTION));
        super.onResume();
        //writeInteractions();
        writePatientInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }


    private void showId() {

        long device_id = preferenceManager.getDeviceId();
        String patient_id = Long.toString(device_id) + Long.toString(computeChecksum(device_id));
        String formattedId = patient_id.replaceAll("..", "$0-");

        CovidDialog alert = new CovidDialog();
        alert.showPatientIdDialog(MainActivity.this, formattedId);
    }

    public void updateMainActivity() {

        writePatientInfo();
        writeQRCode();
    }

    public void writePatientInfo() {

        String dont_panic = getResources().getString(string.dont_panic);
        String call_emergency = getResources().getString(string.call_emergency);

        TextView statusTextView = (TextView) findViewById(id.title);
        TextView descriptionTextView = (TextView) findViewById(id.description);
        LinearLayout back_color = (LinearLayout) findViewById(id.background_color);
        PatientStatus status = new PreferenceManager(getApplicationContext()).getPatientStatus();

        statusTextView.setText(String.valueOf(status.getTitle()));
        descriptionTextView.setText(String.valueOf(status.getDescription()));

        back_color.setBackgroundColor(status.getColor());

        if (status.toInt() > 1) {
            statusTextView.setTextColor(getResources().getColor(R.color.white));
        }
        if (status.toInt() == 1) { // To put the word Coronavirus in red
            statusTextView.setText(Html.fromHtml(status.getTitle()));
        }
        if (status.toInt() == 2 || status.toInt() > 3) {
            descriptionTextView.setText(Html.fromHtml("<b>" + dont_panic + "</b><br/>" +
                    status.getDescription() + "<br/><b>" + call_emergency + "</b>"));
        }
    }

    /*private void writeInteractions() {
        TextView interactionsTextView = (TextView) findViewById(R.id.n_interactions);

        int interactions = new StorageManager(getApplicationContext()).countInteractions();
        interactionsTextView.setText(String.valueOf(interactions));
    }*/

    private void writeQRCode() {
        ImageView qrImage = (ImageView) findViewById(id.qr_code);

        long deviceId = new PreferenceManager(mContext.getApplicationContext()).getDeviceId();

        QRCodeGenerator generator = new QRCodeGenerator(mContext);
        generator.generateQRCode(deviceId, qrImage);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case id.button_show_id:
                TouchListener.buttonClickEffect(v);   //add click button effect
                showId();
                break;

            case id.button_more_info:
                TouchListener.buttonClickEffect(v);   //add click button effect
                startActivity(new Intent(MainActivity.this, MoreInfoActivity.class));
                break;

            case id.how_it_works:
                TouchListener.buttonClickEffect(v);   //add click button effect
                startActivity(new Intent(MainActivity.this, HowItWorksActivity.class));
                break;

            case id.facebook:
                // do your code
                shareToFacebook(MESSAGE, URL);
                break;

            case R.id.twitter:
                shareToTwitter(MESSAGE);
                break;

            case R.id.linkedin:
                shareToLinkedin(MESSAGE, URL);
                break;

            case id.messenger:
                // do your code
                shareToMessenger(MESSAGE);
                break;

            case R.id.whatsapp:
                shareToWhatsapp(MESSAGE);
                break;

            case R.id.sms:
                shareToSMS(MESSAGE);
                break;

            case R.id.mail:
                shareToEmail(LABEL, MESSAGE);
                break;

            case R.id.other:
                shareWith(MESSAGE);
                break;
            default:
                break;
        }
    }

    private void shareToTwitter(String message) {
        Intent intent = null;
        try {
            // get the Twitter app if possible
            getPackageManager().getPackageInfo("com.twitter.android", 0);
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://post?message=" + Uri.encode(message)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } catch (Exception e) {
            // no Twitter app, revert to browser
            String tweetUrl = "https://twitter.com/intent/tweet?text=" + Uri.encode(message);
            Uri uri = Uri.parse(tweetUrl);
            intent = new Intent(Intent.ACTION_VIEW, uri);
        }
        startActivity(intent);
    }

    private void shareToWhatsapp(String message) {
        if (isPackageInstalled("com.whatsapp")) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, message);
            sendIntent.setType("text/plain");
            sendIntent.setPackage("com.whatsapp");

            startActivity(sendIntent);
        } else {
            shareWith(MESSAGE);
        }
    }

    private void shareToEmail(String subject, String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            shareWith(MESSAGE);
        }
    }

    private void shareToSMS(String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:")); // only SMMS apps should handle this
        intent.putExtra("sms_body", message);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            shareWith(MESSAGE);
        }
    }

    private void shareToMessenger(String message) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, MESSAGE);
        sendIntent.setType("text/plain");
        sendIntent.setPackage("com.facebook.orca");
        if (sendIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(sendIntent);
        } else {
            shareWith(MESSAGE);
        }
    }

    private void shareToFacebook(String message, String url) {
        Shareable shareAction = new Shareable.Builder(this)
                .message(message)
                .url(url)
                .socialChannel(Shareable.Builder.FACEBOOK)
                .build();
        shareAction.share();
    }


    private void shareToLinkedin(String message, String url) {
//        if(isPackageInstalled("com.linkedin.android")){
//            Intent shareIntent = new Intent(Intent.ACTION_SEND);
//            shareIntent.setClassName("com.linkedin.android",
//                    "com.linkedin.android.home.UpdateStatusActivity");
//            shareIntent.setType("text/*");
//            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
//            startActivity(shareIntent);
//        } else {
//            shareWith(MESSAGE);
//        }
        Shareable shareAction = new Shareable.Builder(this)
                .message(message)
                .url(url)
                .socialChannel(Shareable.Builder.LINKED_IN)
                .build();
        shareAction.share();
    }


    private void shareWith(String message) {

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }


    public boolean isPackageInstalled(String targetPackage) {
        PackageManager pm = getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    /* Manage back button when is pressed in order to exit from application*/
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        // builder.setCancelable(false);
        builder.setTitle(R.string.alert_exit_title);
        builder.setMessage(R.string.alert_exit_msg);
        builder.setPositiveButton(R.string.alert_exit_pos_bt, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.setNegativeButton(string.alert_exit_neg_bt, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
}
