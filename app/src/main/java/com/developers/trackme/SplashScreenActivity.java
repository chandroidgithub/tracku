package com.developers.trackme;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.developers.trackme.location.GPSTracker;
import com.developers.trackme.myutils.ManagePermission;
import com.developers.trackme.myutils.Utils;

/**
 * Created by android on 8/6/18.
 */

public class SplashScreenActivity extends AppCompatActivity {


    Handler handler = new Handler();
    private int progressStatus = 0;
    private GPSTracker mytracker;
    private Location myLocation;
    private double latitude = 0.0d, longitude = 0.0d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        mytracker = new GPSTracker(SplashScreenActivity.this);

        if (!ManagePermission.checkPermission(SplashScreenActivity.this)) {

            ManagePermission.requestPermission(SplashScreenActivity.this);

        } else if (mytracker.canGetLocation()) {
            myLocation = mytracker.getLocation();
        } else {
            mytracker.showSettingsAlert();
        }


        if (Utils.checkInternetConenction(SplashScreenActivity.this)) {

            new Thread(new Runnable() {
                public void run() {
                    while (progressStatus < 100) {
                        progressStatus += 1;
                        handler.post(new Runnable() {

                            public void run() {

                                //TODO: Do necessary process in this time

                            }

                        });

                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (progressStatus == 100) {


                        Intent i = new Intent(getApplicationContext(), HomeScreenActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(i);
                        finish();
                    }

                }


            }).start();

        } else {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    SplashScreenActivity.this);
            alertDialogBuilder.setTitle("Network Error");
            alertDialogBuilder
                    .setMessage("Please check your Internet Connection");
            alertDialogBuilder.setCancelable(true);

            alertDialogBuilder.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                            dialog.cancel();

                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        }
    }

}
