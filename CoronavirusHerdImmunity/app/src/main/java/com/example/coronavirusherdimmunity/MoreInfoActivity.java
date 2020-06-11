package com.example.coronavirusherdimmunity;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.coronavirusherdimmunity.enums.PatientStatus;
import com.example.coronavirusherdimmunity.models.TouchListener;

public class MoreInfoActivity extends Activity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //Remove title bar
            this.requestWindowFeature(Window.FEATURE_NO_TITLE);
            //Remove notification bar
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            //set content view AFTER ABOVE sequence (to avoid crash)

            setContentView(R.layout.more_info);
            writePatientInfo();

            Button button_back = findViewById(R.id.button_back);
            TouchListener.buttonClickEffect(button_back);   //add click button effect
            button_back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }



    public void writePatientInfo() {

        String dont_panic = getResources().getString(R.string.dont_panic);
        String call_emergency = getResources().getString(R.string.call_emergency);

        TextView descriptionTextView = (TextView) findViewById(R.id.description);
        PatientStatus status = new PreferenceManager(getApplicationContext()).getPatientStatus();

        descriptionTextView.setText(String.valueOf(status.getDescription()));

        if (status.toInt() == 2 || status.toInt() > 3) {
            descriptionTextView.setText(Html.fromHtml("<b>" + dont_panic + "</b><br/>" +
                    status.getDescription() + "<br/><b>" + call_emergency + "</b>"));
        }
    }
}
