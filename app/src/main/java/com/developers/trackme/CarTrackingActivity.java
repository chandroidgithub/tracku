package com.developers.trackme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.developers.trackme.MyApplication.MyApplication;
import com.developers.trackme.location.GPSTracker;
import com.developers.trackme.myutils.ManagePermission;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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
import java.util.List;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Created by android on 4/6/18.
 */

public class CarTrackingActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    public static final String TAG = CarTrackingActivity.class.getSimpleName();
    public static float bearing = 0.0f;
    public Location mylocation;
    public LocationProvider provider;
    public double accuracy = 0.0d;
    public boolean hasAccuracy = false, hasSpeed = false, hasBearing = false;
    public Marker marker;
    public Polyline cyanPolyline;
    public String socket_name = "default", socket_room = "123";
    private int speed = 0;
    private LatLng startPosition, endPosition;
    private double lat = 0.0, lng = 0.0;
    private SupportMapFragment mapFragment;
    private GoogleMap myMap;
    private LatLng myLatLng;
    private Socket socket;
    private List<LatLng> locationHistoryList;
    /* private List<Location> locationList;
     private List<LatLng> polyLineList;*/
    private LocationManager locMan;
    private Criteria criteria;
    public GPSTracker gpsTracker;
    private Handler handler;
    private boolean isMarkerRotating = false;
    private JSONObject data;
    private int index, next;

    //Socket onConnect Event
    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            try {
                JSONObject obj = new JSONObject();

                /*obj.put("name", "cartrack");
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

    //Getting Response from Socket
    private Emitter.Listener getlatlong = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            CarTrackingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    try {

                        data = (JSONObject) args[0];

                        if (data != null) {

                            JSONObject message = data.getJSONObject("message");

                            lat = Double.parseDouble(message.getString("lat"));

                            Log.i("slat", ":: " + lat);

                            lng = Double.parseDouble(message.getString("lng"));

                            Log.i("slong", ":: " + lng);

                            locationHistoryList.add(new LatLng(lat, lng));

                            Log.i("location_size", "[" + locationHistoryList.size() + "]");

                        }

                    } catch (JSONException e) {

                        return;
                    }

                    if (locationHistoryList.size() > 1) {


                        Log.i("start_end", "::" + locationHistoryList.get(locationHistoryList.size() - 2) + ">>" + locationHistoryList.get(locationHistoryList.size() - 1));

                        boolean hasAccuracy = mylocation.hasAccuracy();

                        Log.i("hasAccuracy", ">>"+hasAccuracy);

                        if(hasAccuracy) {

                            PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
                            for (int z = 0; z < locationHistoryList.size() - 1; z++) {
                                LatLng point = locationHistoryList.get(z);
                                options.add(point);
                            }
                            cyanPolyline = myMap.addPolyline(options);
                        }



                        animateMarker(marker, locationHistoryList.get(locationHistoryList.size() - 1), false);
                        //marker.setPosition(locationHistoryList.get(locationHistoryList.size() - 1));
                        double bearing = getBearing(locationHistoryList.get(locationHistoryList.size() - 2), locationHistoryList.get(locationHistoryList.size() - 1));
                        Log.i("bearing", "::" + bearing);

                        if (bearing > 20) {
                            //marker.setRotation((float) bearing);
                            rotateMarker(marker, (float) bearing);
                        }
                        marker.setAnchor(0.5f, 0.5f);
                        marker.setFlat(false);
                        myMap.moveCamera(CameraUpdateFactory
                                .newCameraPosition
                                        (new CameraPosition.Builder()
                                                .target(locationHistoryList.get(locationHistoryList.size() - 1))
                                                .zoom(15.5f)
                                                .build()));


                    }
                }

            });
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_tracking);

        if (getIntent() != null) {
            socket_name = getIntent().getStringExtra("sname");
            socket_room = getIntent().getStringExtra("sroom");
        }


        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        //Store Location Points for Markers
        locationHistoryList = new ArrayList<>();


        //Initialization of GPSTracker

        GPSTracker gpsTracker = new GPSTracker(CarTrackingActivity.this);

        //Initialization of Socket.io

        MyApplication app = (MyApplication) getApplication();

        socket = app.getSocket();

        socket.on(Socket.EVENT_CONNECT, onConnect);

        socket.on("getlatlong", getlatlong);

        socket.connect();

        //-----------------------------------------


        if (ManagePermission.checkPermission(CarTrackingActivity.this)) {


            if (gpsTracker.canGetLocation() == false) {

                gpsTracker.showSettingsAlert();

            } else {
                locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                criteria.setPowerRequirement(Criteria.POWER_HIGH);
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.setAltitudeRequired(false);
                criteria.setBearingRequired(true);
                criteria.setCostAllowed(true);
                criteria.setSpeedRequired(true);
                String bestProvider = locMan.getBestProvider(criteria, true);
                locMan.requestLocationUpdates(bestProvider, 5000, 5, this);
                locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2, this);
                locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 2, this);
                mylocation = locMan.getLastKnownLocation(bestProvider);
                //myLatLng = new LatLng(gpsTracker.getLatitude(), gpsTracker.getLongitude());
                if (mylocation != null) {
                    myLatLng = new LatLng(mylocation.getLatitude(), mylocation.getLongitude());
                }
            }
        } else {
            ManagePermission.requestPermission(CarTrackingActivity.this);
        }


    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationHistoryList.clear();
        socket.disconnect();
        socket.close();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (myMap == null) {

            myMap = googleMap;

        }

        if (ManagePermission.checkPermission(CarTrackingActivity.this)) {

            setupMap(mapFragment, myMap);

            marker = myMap.addMarker(new MarkerOptions()
                    .position(myLatLng)
                    .flat(false)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_cab)));

            myMap.moveCamera(CameraUpdateFactory.newLatLng(myLatLng));
            myMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(googleMap.getCameraPosition().target)
                    .zoom(17)
                    .bearing(30)
                    .tilt(45)
                    .build()));

        } else {
            ManagePermission.requestPermission(CarTrackingActivity.this);
        }

        /*//Drag Marker to desired position
        central_marker = (ImageView)findViewById(R.id.central_marker);
        final int[] init_loc = {0};
        final int final_loc = -50;
        //myMap = googleMap;

        final CountDownTimer timer = new CountDownTimer(300,300) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                init_loc[0] = 0;
                ObjectAnimator objectAnimatorY = ObjectAnimator.ofFloat(central_marker, "translationY", final_loc, init_loc[0]);
                objectAnimatorY.setDuration(200);
                objectAnimatorY.start();
            }
        };


        myMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {

                System.out.println("Camera started moving worked");
                timer.cancel();
                ObjectAnimator objectAnimatorY = ObjectAnimator.ofFloat(central_marker, "translationY", init_loc[0], final_loc);
                objectAnimatorY.setDuration(200);
                objectAnimatorY.start();
                init_loc[0] = -50;

            }
        });



        myMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                System.out.println("Camera idle worked");

                if(initial_flag!=0)
                {
                    System.out.println("Camera Setting timer now");
                    timer.cancel();
                    timer.start();
                }
                initial_flag++;
                System.out.println("Camera Value of initial_flag ="+initial_flag);
            }
        });*/

    }


    //SETUP GOOGLE MAP
    @SuppressLint("MissingPermission")
    private void setupMap(SupportMapFragment sf, GoogleMap googleMap) {

        if (myMap == null) {

            myMap = googleMap;

            sf = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            sf.getMapAsync(this);
        }

        myMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Enable MyLocation Button in the Map
        myMap.setMyLocationEnabled(true);

        myMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Zoom level 15
        myMap.animateCamera(CameraUpdateFactory.zoomTo(17));

        // Enable / Disable zooming controls
        myMap.getUiSettings().setZoomControlsEnabled(true);

        // Enable / Disable my location button
        myMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Enable / Disable Compass icon
        myMap.getUiSettings().setCompassEnabled(true);

        // Enable / Disable Rotate gesture
        myMap.getUiSettings().setRotateGesturesEnabled(true);

        // Enable / Disable zooming functionality
        myMap.getUiSettings().setZoomGesturesEnabled(true);

        myMap.setTrafficEnabled(false);

        myMap.setIndoorEnabled(false);

        myMap.setBuildingsEnabled(false);

        myMap.getUiSettings().setZoomControlsEnabled(true);

    }

    @Override
    public void onLocationChanged(Location location) {

        if (ManagePermission.checkPermission(CarTrackingActivity.this)) {

                locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                criteria.setPowerRequirement(Criteria.POWER_HIGH);
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.setAltitudeRequired(false);
                criteria.setBearingRequired(true);
                criteria.setCostAllowed(true);
                criteria.setSpeedRequired(true);
                String bestProvider = locMan.getBestProvider(criteria, true);
                locMan.requestLocationUpdates(bestProvider, 5000, 5, this);
                locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2, this);
                locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 2, this);

                if (location != null) {
                    mylocation = locMan.getLastKnownLocation(bestProvider);
                    //myLatLng = new LatLng(gpsTracker.getLatitude(), gpsTracker.getLongitude());
                    myLatLng = new LatLng(mylocation.getLatitude(), mylocation.getLongitude());
                }

        } else {
            ManagePermission.requestPermission(CarTrackingActivity.this);
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private float getBearing(LatLng begin, LatLng end) {

        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
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

    //MARKAR ANIMATION
    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = myMap.getProjection();
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
