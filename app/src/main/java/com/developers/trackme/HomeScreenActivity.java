package com.developers.trackme;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.developers.trackme.location.GPSTracker;
import com.developers.trackme.myutils.ManagePermission;

/**
 * Created by android on 7/6/18.
 */

public class HomeScreenActivity extends AppCompatActivity {

    EditText et_name, et_room;
    Double lat = 0.0d, lng = 0.0d;
    private GPSTracker gpsTracker;
    private Button btn_tracku, btn_utrack, btn_direction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        gpsTracker = new GPSTracker(HomeScreenActivity.this);


        btn_tracku = (Button) findViewById(R.id.btn_trackyou);
        btn_utrack = (Button) findViewById(R.id.btn_trackme);
        btn_direction = (Button) findViewById(R.id.btn_direction);

        et_name = (EditText) findViewById(R.id.et_name);
        et_room = (EditText) findViewById(R.id.et_room);


        if (!ManagePermission.checkPermission(HomeScreenActivity.this)) {
            ManagePermission.requestPermission(HomeScreenActivity.this);
        } else {

            if (gpsTracker.canGetLocation()) {

                Toast.makeText(getApplicationContext(), "Location is available", Toast.LENGTH_LONG).show();
            } else {
                gpsTracker.showSettingsAlert();
            }
        }


        btn_utrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(et_name.getText().toString().trim().length()<1) {
                    et_name.setError("Please Enter your name");

                }else if (et_room.getText().toString().trim().length()<1){

                    et_name.setError("Please Enter room name");

                }else {

                    Intent i = new Intent(HomeScreenActivity.this, LocationGet.class);
                    i.putExtra("sname", et_name.getText().toString().trim());
                    i.putExtra("sroom", et_room.getText().toString().trim());
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }
            }
        });

        btn_tracku.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(et_name.getText().toString().trim().length()<1) {
                    et_name.setError("Please Enter your name");

                }else if (et_room.getText().toString().trim().length()<1){

                    et_name.setError("Please Enter room name");

                }else {
                    Intent i = new Intent(HomeScreenActivity.this, CarTrackingActivity.class);
                    i.putExtra("sname", et_name.getText().toString().trim());
                    i.putExtra("sroom", et_room.getText().toString().trim());
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }
            }
        });

        btn_direction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeScreenActivity.this, DirectionActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });


    }
}
