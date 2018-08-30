package com.developers.trackme;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.developers.trackme.MyApplication.MyApplication;
import com.developers.trackme.myutils.ManagePermission;
import com.developers.trackme.services.LocationMonitoringService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class LocationGet extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private static final String TAG = LocationGet.class.getSimpleName();

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    public Marker marker;
    public GoogleMap mymap;
    public SupportMapFragment sf;
    public Location mylocation;
    public LatLng myLatLng;
    public Polyline cyanPolyline;
    public String socket_name = "", socket_room = "";
    public String bestProvider = "";
    private boolean mAlreadyStartedService = false;
    private TextView mMsgView;
    private boolean isMarkerRotating = false;
    private ArrayList<LatLng> location_list;
    private LocationManager locationManager;
    private Socket socket;
    //Socket onConnect Event
    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            try {

                JSONObject obj = new JSONObject();

                /*obj.put("name", "trackme");
                obj.put("room", "9474094290");*/

                obj.put("name", socket_name);
                obj.put("room", socket_room);

                socket.emit("join", obj);

                Log.i("joined", "socket connected");

            } catch (JSONException je) {
                je.printStackTrace();
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locationget);
        mMsgView = (TextView) findViewById(R.id.msgView);


        if (getIntent() != null) {
            socket_name = getIntent().getStringExtra("sname");
            socket_room = getIntent().getStringExtra("sroom");
        }

        //Socket.io initialization

        MyApplication app = (MyApplication) getApplication();

        socket = app.getSocket();

        socket.on(Socket.EVENT_CONNECT, onConnect);

        //socket.on("setlatlong", sendlatlong);

        socket.connect();


        //------------------------

        location_list = new ArrayList<LatLng>();

        sf = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        sf.getMapAsync(this);

        if (ManagePermission.checkPermission(LocationGet.this)) {

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(true);
            criteria.setCostAllowed(true);
            criteria.setSpeedRequired(true);

            bestProvider = locationManager.getBestProvider(criteria, true);
            mylocation = locationManager.getLastKnownLocation(bestProvider);
            locationManager.requestLocationUpdates(bestProvider, 5000, 5, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 2, this);

        } else {
            ManagePermission.requestPermission(LocationGet.this);
        }


        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        String latitude = intent.getStringExtra(LocationMonitoringService.EXTRA_LATITUDE);
                        String longitude = intent.getStringExtra(LocationMonitoringService.EXTRA_LONGITUDE);

                        if (latitude != null && longitude != null) {

                           /* Location locnow = new Location(bestProvider);

                            locnow.setLatitude(Double.parseDouble(latitude));
                            locnow.setLongitude(Double.parseDouble(latitude));*/

                            mMsgView.setText("Latitude : " + latitude + " | Longitude: " + longitude);

                            Log.i(TAG, "Latitude : " + latitude + "Longitude: " + longitude);

                            location_list.add(new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude)));

                            Log.i(TAG, "[" + location_list.size() + "]");

                            /*mylocation = locnow;*/

                            double accuracy = mylocation.getAccuracy();

                            boolean hasAccuracy = mylocation.hasAccuracy();

                            Log.i(TAG, "accuracy" + ">>" + accuracy + ">" + hasAccuracy);

                            int speed = (int) mylocation.getSpeed();

                            boolean hasSpeed = mylocation.hasSpeed();

                            Log.i(TAG, "speed" + ">>" + speed + ">" + hasSpeed);

                            double bearing = mylocation.getBearing();

                            boolean hasBearing = mylocation.hasBearing();

                            Log.i(TAG, "bearing" + ">>" + bearing + ">" + hasBearing);


                            if (location_list.size() > 1) {

                                //-----Emit Data to Socket-------

                                JSONObject obj = new JSONObject();

                                try {

                                    obj.put("lat", location_list.get(location_list.size() - 1).latitude);

                                    obj.put("lng", location_list.get(location_list.size() - 1).longitude);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                socket.emit("sendlatlong", obj);

                                Log.i("emit", "success");

                                //--------------------------------

                                if (hasAccuracy) {

                                    PolylineOptions options = new PolylineOptions().width(5).color(Color.CYAN).geodesic(true);
                                    for (int z = 0; z < location_list.size() - 1; z++) {
                                        LatLng point = location_list.get(z);
                                        options.add(point);
                                    }
                                    cyanPolyline = mymap.addPolyline(options);

                                }


                                //mymap.addMarker(new MarkerOptions().position(location_list.get(location_list.size()-2)));
                                animateMarker(marker, location_list.get(location_list.size() - 1), false);
                                //marker.setPosition(location_list.get(location_list.size() - 1));

                                float cbearing = (float) bearingBetweenLocations(location_list.get(location_list.size() - 2), location_list.get(location_list.size() - 1));
                                Log.i("cbearing", ">>" + cbearing);

                                if (cbearing > 20) {
                                    //marker.setRotation((float) bearing);
                                    rotateMarker(marker, cbearing);
                                }


                                marker.setAnchor(0.5f, 0.5f);

                                mymap.moveCamera(CameraUpdateFactory.newLatLng(location_list.get(location_list.size() - 1)));
                                mymap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                                        .target(mymap.getCameraPosition().target)
                                        .zoom(17)
                                        //.bearing(30)
                                        .tilt(45)
                                        .build()));

                                //Place current location marker
                                /*LatLng latLng = new LatLng(mylocation.getLatitude(), mylocation.getLongitude());
                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(latLng);
                                markerOptions.title("Current Position");
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                                marker = mymap.addMarker(markerOptions);

                                //move map camera
                                mymap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                mymap.animateCamera(CameraUpdateFactory.zoomTo(11));*/

                           /* if (mGoogleApiClient != null)
                            {
                                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                            }*/

                            }

                        }
                    }
                }, new IntentFilter(LocationMonitoringService.ACTION_LOCATION_BROADCAST)
        );
    }


    @Override
    public void onResume() {
        super.onResume();

        startStep1();
    }


    /**
     * Step 1: Check Google Play services
     */
    private void startStep1() {

        //Check whether this user has installed Google play service which is being used by Location updates.
        if (isGooglePlayServicesAvailable()) {

            //Passing null to indicate that it is executing for the first time.
            startStep2(null);

        } else {
            Toast.makeText(getApplicationContext(), R.string.no_google_playservice_available, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Step 2: Check & Prompt Internet connection
     */
    private Boolean startStep2(DialogInterface dialog) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            promptInternetConnect();
            return false;
        }


        if (dialog != null) {
            dialog.dismiss();
        }

        //Yes there is active internet connection. Next check Location is granted by user or not.

        if (checkPermissions()) { //Yes permissions are granted by the user. Go to the next step.
            startStep3();
        } else {  //No user has not granted the permissions yet. Request now.
            requestPermissions();
        }
        return true;
    }

    /**
     * Show A Dialog with button to refresh the internet state.
     */
    private void promptInternetConnect() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LocationGet.this);
        builder.setTitle(R.string.title_alert_no_intenet);
        builder.setMessage(R.string.msg_alert_no_internet);

        String positiveText = getString(R.string.btn_label_refresh);
        builder.setPositiveButton(positiveText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                        //Block the Application Execution until user grants the permissions
                        if (startStep2(dialog)) {

                            //Now make sure about location permission.
                            if (checkPermissions()) {

                                //Step 2: Start the Location Monitor Service
                                //Everything is there to start the service.
                                startStep3();
                            } else if (!checkPermissions()) {
                                requestPermissions();
                            }

                        }
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Step 3: Start the Location Monitor Service
     */
    private void startStep3() {

        //And it will be keep running until you close the entire application from task manager.
        //This method will executed only once.

        if (!mAlreadyStartedService && mMsgView != null) {

            mMsgView.setText(R.string.msg_location_service_started);

            //Start location sharing service to app server.........
            Intent intent = new Intent(this, LocationMonitoringService.class);
            startService(intent);

            mAlreadyStartedService = true;
            //Ends................................................
        }
    }

    /**
     * Return the availability of GooglePlayServices
     */
    public boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(this, status, 2404).show();
            }
            return false;
        }
        return true;
    }


    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState1 = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        int permissionState2 = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        return permissionState1 == PackageManager.PERMISSION_GRANTED && permissionState2 == PackageManager.PERMISSION_GRANTED;

    }

    /**
     * Start permissions requests.
     */
    private void requestPermissions() {

        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        boolean shouldProvideRationale2 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);


        // Provide an additional rationale to the img_user. This would happen if the img_user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale || shouldProvideRationale2) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(LocationGet.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the img_user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(LocationGet.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }


    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If img_user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Log.i(TAG, "Permission granted, updates requested, starting location updates");
                startStep3();

            } else {
                // Permission denied.

                // Notify the img_user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the img_user for permission (device policy or "Never ask
                // again" prompts). Therefore, a img_user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }


    @Override
    public void onDestroy() {

        //Stop location sharing service to app server.........
        stopService(new Intent(this, LocationMonitoringService.class));
        mAlreadyStartedService = false;
        //Ends................................................
        location_list.clear();
        socket.disconnect();
        socket.close();

        super.onDestroy();
    }

    private void rotateMarker(final Marker marker, final float toRotation) {
        if (!isMarkerRotating) {
            final Handler handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final float startRotation = marker.getRotation();
            final long duration = 1000;

            final Interpolator interpolator = new LinearInterpolator();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    isMarkerRotating = true;

                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = interpolator.getInterpolation((float) elapsed / duration);

                    float rot = t * toRotation + (1 - t) * startRotation;

                    marker.setRotation(-rot > 180 ? rot / 2 : rot);
                    if (t < 1.0) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16);
                    } else {
                        isMarkerRotating = false;
                    }
                }
            });
        }
    }

    private double bearingBetweenLocations(LatLng latLng1, LatLng latLng2) {

        double PI = 3.14159;
        double lat1 = latLng1.latitude * PI / 180;
        double long1 = latLng1.longitude * PI / 180;
        double lat2 = latLng2.latitude * PI / 180;
        double long2 = latLng2.longitude * PI / 180;

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;

        return brng;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (mymap == null) {

            mymap = googleMap;
        }


        if (ManagePermission.checkPermission(LocationGet.this)) {

            setupMap(sf, mymap);

            MarkerOptions markerOptions = new MarkerOptions();

            if (mylocation != null) {
                markerOptions.position(new LatLng(mylocation.getLatitude(), mylocation.getLongitude()));
            }
            markerOptions.title("Start Position");
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_cab));
            marker = mymap.addMarker(markerOptions);

        } else {
            ManagePermission.requestPermission(LocationGet.this);
        }

    }

    @Override
    public void onLocationChanged(Location location) {

        if (location != null) {

            myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLatLng, 17);
            mymap.animateCamera(cameraUpdate);
            locationManager.removeUpdates(this);

        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Toast.makeText(LocationGet.this, provider + " status changed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(LocationGet.this, provider + " has enabled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(LocationGet.this, provider + " has enabled", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("MissingPermission")
    private void setupMap(SupportMapFragment sf, GoogleMap googleMap) {

        mymap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Enable MyLocation Button in the Map
        mymap.setMyLocationEnabled(true);

        mymap.getUiSettings().setMyLocationButtonEnabled(true);

        // Zoom level 15
        mymap.animateCamera(CameraUpdateFactory.zoomTo(17));

        // Enable / Disable zooming controls
        mymap.getUiSettings().setZoomControlsEnabled(true);

        // Enable / Disable my location button
        mymap.getUiSettings().setMyLocationButtonEnabled(true);

        // Enable / Disable Compass icon
        mymap.getUiSettings().setCompassEnabled(true);

        // Enable / Disable Rotate gesture
        mymap.getUiSettings().setRotateGesturesEnabled(true);

        // Enable / Disable zooming functionality
        mymap.getUiSettings().setZoomGesturesEnabled(true);

        mymap.setTrafficEnabled(false);

        mymap.setIndoorEnabled(false);

        mymap.setBuildingsEnabled(false);

        mymap.getUiSettings().setZoomControlsEnabled(true);

    }

    //MARKAR ANIMATION
    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mymap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 1000;
        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;

                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));
                //marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.taxi_top));
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }
}
