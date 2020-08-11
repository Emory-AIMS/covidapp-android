package com.example.coronavirusherdimmunity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.coronavirusherdimmunity.enums.PatientStatus;
import com.example.coronavirusherdimmunity.models.CovidDialog;
import com.example.coronavirusherdimmunity.utils.ApiManager;

import bolts.Continuation;
import bolts.Task;

import java.util.HashMap;
import java.util.concurrent.Callable;
import okhttp3.Response;
import android.util.Log;
import org.json.JSONObject;

import com.example.coronavirusherdimmunity.models.TouchListener;

public class ReportStatusActivity extends Activity {

    private Button bt_confirm_covid, bt_negative_covid, bt_recover_covid, bt_back;
    private PreferenceManager preferenceManager;
    private long device_id;
    private boolean task_activated_updateUserStatus = true;               //used to call just one by one the task "task_updateUserStatus"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_status);

        preferenceManager = new PreferenceManager(this);
        device_id = preferenceManager.getDeviceId();


        /****************** Confirm Button *******************/

        bt_confirm_covid = findViewById(R.id.bt_confirm_covid);
        TouchListener.buttonClickEffect(bt_confirm_covid);   //add click button effect
        bt_confirm_covid.setOnClickListener(new View.OnClickListener() {

            @Override
            /**
             * Click on "Positive to Covid 19" button, change patient's status to 'infected'
             */
            public void onClick(View view)
            {

                if (task_activated_updateUserStatus) { //used to call just one by one the task "task_updateUserStatus"
                    task_activated_updateUserStatus = false; //become false so that task "task_updateUserStatus" cannot be called again

                    task_updateUserStatus(device_id, "INFECTED");     //"infected"
                }

            }
        });

        /****************** Negative Button *******************/

        bt_negative_covid = findViewById(R.id.bt_negative_covid);
        TouchListener.buttonClickEffect(bt_negative_covid);   //add click button effect
        bt_negative_covid.setOnClickListener(new View.OnClickListener() {

            @Override
            /**
             * Click on "Negative to Covid-19" button, change patient's status to 'negative'
             */
            public void onClick(View view)
            {
                task_updateUserStatus(device_id, "NORMAL");     //"negative"
            }
        });


        /****************** Recover Button *******************/

        bt_recover_covid = findViewById(R.id.bt_recover_covid);
        TouchListener.buttonClickEffect(bt_recover_covid);   //add click button effect
        bt_recover_covid.setOnClickListener(new View.OnClickListener() {

            @Override
            /**
             * Click on "Healed" button, change patient's status to 'healed'
             */
            public void onClick(View view)
            {
                if (task_activated_updateUserStatus) { //used to call just one by one the task "task_updateUserStatus"
                    task_activated_updateUserStatus = false; //become false so that task "task_updateUserStatus" cannot be called again

                    task_updateUserStatus(device_id, "HEALED");        //"healed"

                }
            }
        });

        /****************** Back Button *******************/

        bt_back = findViewById(R.id.button_back);
        bt_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }


    private static final HashMap<String, Integer> status_map= new HashMap<>();
    static {
        status_map.put("INFECTED", R.string.status_infected);
        status_map.put("NORMAL", R.string.status_negative);
        status_map.put("HEALED", R.string.status_healed);
    };

    private void task_updateUserStatus(final long user_id, final String str_new_status){

        Toast.makeText(getApplicationContext(), R.string.toast_click_status_changed, Toast.LENGTH_SHORT).show();

        Task.callInBackground(new Callable<String>() {
            @Override
            public String call() throws Exception {

                Integer new_status = PatientStatus.valueOf(str_new_status).ordinal();     // get 'enum' value of PatientStatus

                Boolean updated =  false;
                String ret_value = "";

                while ( updated == false){

                    Response response_updateUS = ApiManager.updateUserStatus(user_id, new_status, preferenceManager.getAuthToken());  //call updateUserStatus

                    if (response_updateUS != null) {
                        switch (response_updateUS.code()) {//check response status(code)
                            case 200:     // if response is 'ok' -> status has been changed
                                Log.d("task_updateUserStatus","status has been changed");
                                ret_value = "chg_st";
                                updated = true; //exit while
                                break;
                            case 401:     // if jwt token is expired -> call refreshJwtToken and recall task_updateUserStatus
                                Log.d("task_updateUserStatus","Jwt Token expired");

                                String token =  new ApiManager.HttpInterceptor().refreshToken();  //call refreshToken

                                break;
                            default:       // for example, if patient id is not recognized
                                Log.d("task_updateUserStatus", "Code not recognized:"+response_updateUS.code());
                                ret_value = "not_rec";
                                updated = true; //exit while
                                break;
                        }
                    } else{  // no response from Backend (like: internet disabled)
                        Log.d("task_updateUserStatus","No response by updateUserStatus");
                        ret_value = "no_resp";
                        updated = true; //exit while
                    }
                }

                return ret_value;
            }
        }).onSuccess(new Continuation<String, Object>() {
            @Override
            public String then(Task<String> task) throws Exception {

                switch (task.getResult()) {
                    case "chg_st":  //health status changed
                        String msg = String.format(getString(R.string.aldiag_status_changed), getString(status_map.get(str_new_status))); //add 'new status' on string
                        CovidDialog.warningDialog(ReportStatusActivity.this, getString(R.string.aldiag_title_change_status), msg); //display warning dialog
                        break;
                    case "no_resp": // no response from Backend (like: internet disabled)
                        CovidDialog.warningDialog(ReportStatusActivity.this, getString(R.string.aldiag_title_change_status), getString(R.string.aldiag_no_resp_change_status)); //display warning dialog
                        break;
                    default: //some errors
                        CovidDialog.warningDialog(ReportStatusActivity.this, getString(R.string.aldiag_title_change_status), getString(R.string.aldiag_err_status_change)); //display warning dialog
                        break;
                }

                task_activated_updateUserStatus = true;    //become true so that task "task_updateUserStatus" can be called again
                return null;
            }
        },  Task.UI_THREAD_EXECUTOR);
    }



}
