package com.akaver.gmapdemo02;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private final static String TAG = "MainActivity";

    private GoogleMap mGoogleMap;
    private Menu mOptionsMenu;
    private BroadcastReceiver mBroadcastReceiver;

    private LocationManager locationManager;

    private String provider;

    private int markerCount = 0;
    private Location locationPrevious;
    private Location locationWaypoint;
    private Location locationCountReset;

    private double totalDistance;
    private double waypointDistance;
    private double countResetDistance;

    private Polyline mPolyline;
    private PolylineOptions mPolylineOptions;


    private TextView textViewWPCount;
    private TextView textViewSpeed;

    private TextView textViewCountResetDistance;
    private TextView textViewLineCountResetDistance;

    private TextView textViewWPDistance;
    private TextView textViewLineWPDistance;

    private TextView textViewTotalDistance;
    private TextView textViewLineDistance;

    private double lineDistance;
    private double lineWPDistance;
    private double lineCRDistance;

    private Location locationInitial;

    private boolean initialLocationSet = false;

    private NotificationManager mNotificationManager;

    private Date datePrevious;
    private String speedString = "0:00";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();

        // get the location provider (GPS/CEL-towers, WIFI)
        provider = locationManager.getBestProvider(criteria, false);

        //Log.d(TAG, provider);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");

        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");

        }

        locationPrevious = locationManager.getLastKnownLocation(provider);

        if (locationPrevious != null) {
            // do something with initial position?
        }

        textViewWPCount = (TextView) findViewById(R.id.textview_wpcount);
        textViewSpeed = (TextView) findViewById(R.id.textview_speed);
        textViewTotalDistance = (TextView) findViewById(R.id.textview_total_distance);
        textViewLineDistance = (TextView) findViewById(R.id.textview_total_line);
        textViewCountResetDistance = (TextView) findViewById(R.id.textview_creset_distance);
        textViewLineCountResetDistance = (TextView) findViewById(R.id.textview_creset_line);
        textViewWPDistance = (TextView) findViewById(R.id.textview_wp_distance);
        textViewLineWPDistance = (TextView) findViewById(R.id.textview_wp_line);



        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("Intent");

                switch (intent.getAction()) {
                    case "notification-broadcast-resettripmeter":
                        buttonCResetClicked(null);
                        return;
                    case "notification-broadcast-addwaypoint":
                        buttonAddWayPointClicked(null);
                        return;
                    default:
                        return;

            }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("notification-broadcast");
        intentFilter.addAction("notification-broadcast-addwaypoint");
        intentFilter.addAction("notification-broadcast-resettripmeter");
        registerReceiver(mBroadcastReceiver, intentFilter);

        buttonNotificationCustomLayout();
    }

    public void buttonNotificationCustomLayout() {

        // get the view layout
        RemoteViews remoteView = new RemoteViews(
                getPackageName(), R.layout.notification);

        // define intents
        PendingIntent pIntentAddWaypoint = PendingIntent.getBroadcast(
                this,
                0,
                new Intent("notification-broadcast-addwaypoint"),
                0
        );

        PendingIntent pIntentResetTripmeter = PendingIntent.getBroadcast(
                this,
                0,
                new Intent("notification-broadcast-resettripmeter"),
                0
        );

        // bring back already running activity
        // in manifest set android:launchMode="singleTop"
        PendingIntent pIntentOpenActivity = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // attach events
        remoteView.setOnClickPendingIntent(R.id.buttonAddWayPoint, pIntentAddWaypoint);
        remoteView.setOnClickPendingIntent(R.id.buttonResetTripmeter, pIntentResetTripmeter);
        remoteView.setOnClickPendingIntent(R.id.buttonOpenActivity, pIntentOpenActivity);

        remoteView.setTextViewText(R.id.textViewWayPointMetrics, String.format("%.0f", waypointDistance));
        remoteView.setTextViewText(R.id.textViewTripmeterMetrics, String.format("%.0f", countResetDistance));

        // build notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContent(remoteView)
                        .setSmallIcon(R.drawable.ic_info_outline_white_24dp);

        // notify
        mNotificationManager.notify(4, mBuilder.build());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mOptionsMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.menu_mylocation:
                item.setChecked(!item.isChecked());
                updateMyLocation();
                return true;
            case R.id.menu_trackposition:
                item.setChecked(!item.isChecked());
                updateTrackPosition();
                return true;
            case R.id.menu_keepmapcentered:
                item.setChecked(!item.isChecked());
                return true;
            case R.id.menu_map_type_hybrid:
            case R.id.menu_map_type_none:
            case R.id.menu_map_type_normal:
            case R.id.menu_map_type_satellite:
            case R.id.menu_map_type_terrain:
                item.setChecked(true);
                updateMapType();
                return true;

            case R.id.menu_map_zoom_10:
            case R.id.menu_map_zoom_15:
            case R.id.menu_map_zoom_20:
            case R.id.menu_map_zoom_in:
            case R.id.menu_map_zoom_out:
            case R.id.menu_map_zoom_fittrack:
                updateMapZoomLevel(item.getItemId());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }


    }


    private void updateMapZoomLevel(int itemId) {
        if (!checkReady()) {
            return;
        }

        switch (itemId) {
            case R.id.menu_map_zoom_10:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(10));
                break;
            case R.id.menu_map_zoom_15:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                break;
            case R.id.menu_map_zoom_20:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(20));
                break;
            case R.id.menu_map_zoom_in:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomIn());
                break;
            case R.id.menu_map_zoom_out:
                mGoogleMap.moveCamera(CameraUpdateFactory.zoomOut());
                break;
            case R.id.menu_map_zoom_fittrack:
                updateMapZoomFitTrack();
                break;
        }
    }

    private void updateMapZoomFitTrack() {
        if (mPolyline == null) {
            return;
        }

        List<LatLng> points = mPolyline.getPoints();

        if (points.size() <= 1) {
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        int padding = 0; // offset from edges of the map in pixels
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

    }

    private void updateTrackPosition() {
        if (!checkReady()) {
            return;
        }

        if (mOptionsMenu.findItem(R.id.menu_trackposition).isChecked()) {
            mPolylineOptions = new PolylineOptions().width(50).color(Color.parseColor("#9CCC65"));
            mPolyline = mGoogleMap.addPolyline(mPolylineOptions);
        }


    }

    private void updateMapType() {
        if (!checkReady()) {
            return;
        }

        if (mOptionsMenu.findItem(R.id.menu_map_type_normal).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_hybrid).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_none).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_satellite).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else if (mOptionsMenu.findItem(R.id.menu_map_type_terrain).isChecked()) {
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }

    }

    private boolean checkReady() {
        if (mGoogleMap == null) {
            Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void updateMyLocation() {
        if (mOptionsMenu.findItem(R.id.menu_mylocation).isChecked()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mGoogleMap.setMyLocationEnabled(true);
            return;
        }

        mGoogleMap.setMyLocationEnabled(false);
    }

    public void buttonAddWayPointClicked(View view){
        if (locationPrevious==null){
            return;
        }

        locationWaypoint = locationPrevious;
        waypointDistance = 0;

        markerCount++;

        mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(locationPrevious.getLatitude(), locationPrevious.getLongitude())).title(Integer.toString(markerCount)));
        textViewWPCount.setText(Integer.toString(markerCount));

    }

    public void buttonCResetClicked(View view){

        locationCountReset = locationPrevious;
        countResetDistance = 0;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        //mGoogleMap.setMyLocationEnabled(false);

        //LatLng latLngITK = new LatLng(59.3954789, 24.6621282);
        //mGoogleMap.addMarker(new MarkerOptions().position(latLngITK).title("ITK"));
        //mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngITK, 17));

        // set zoom level to 15 - street
        mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(17));

        // if there was initial location received, move map to it
        if (locationPrevious != null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(locationPrevious.getLatitude(), locationPrevious.getLongitude())));
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }

    @Override
    public void onLocationChanged(Location location) {

        if (mGoogleMap==null) return;

        if (locationPrevious == null) {
            locationPrevious = location;
            return;
        }

        //initial values to location variables
        if (!initialLocationSet && locationPrevious != null){
            locationInitial = locationPrevious;
            locationWaypoint = locationPrevious;
            locationCountReset = locationPrevious;
            initialLocationSet = true;
        }

        //speed calculations
        Date currentDate = new Date();

        if (datePrevious != null) {
            double distance = location.distanceTo(locationPrevious);
            double time = (currentDate.getTime() - datePrevious.getTime()) / 1000;
            double speedMetersSecond = distance / time;

            if(Double.isInfinite(speedMetersSecond) || Double.isNaN(speedMetersSecond)) {}
            else {
                speedString = calculateSpeed(speedMetersSecond);
            }
        }

        //distance calculations
        totalDistance = totalDistance + location.distanceTo(locationPrevious);
        waypointDistance = waypointDistance + location.distanceTo(locationPrevious);
        countResetDistance = countResetDistance + location.distanceTo(locationPrevious);

        lineDistance =  location.distanceTo(locationInitial);
        lineWPDistance =  location.distanceTo(locationWaypoint);
        lineCRDistance =  location.distanceTo(locationCountReset);


        //menu center map and track positions
        LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());

        if (mOptionsMenu.findItem(R.id.menu_keepmapcentered).isChecked() || locationPrevious == null) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(newPoint));
        }

        if (mOptionsMenu.findItem(R.id.menu_trackposition).isChecked()) {
            List<LatLng> points = mPolyline.getPoints();
            points.add(newPoint);
            mPolyline.setPoints(points);
        }

        //view values
        textViewTotalDistance.setText(String.format("%.0f", totalDistance));
        textViewLineDistance.setText(String.format("%.0f", lineDistance));

        textViewCountResetDistance.setText(String.format("%.0f", countResetDistance));
        textViewLineCountResetDistance.setText(String.format("%.0f", lineCRDistance));

        textViewWPDistance.setText(String.format("%.0f", waypointDistance));
        textViewLineWPDistance.setText(String.format("%.0f", lineWPDistance));

        textViewSpeed.setText(speedString);

        //override previous location, date
        locationPrevious = location;
        datePrevious = currentDate;

        buttonNotificationCustomLayout();
    }

    private String calculateSpeed(double metersPerSecond) {
//        1 kilometer/minute = 16.6666666666666666666667 meter/second
//        x = (1 * metersPerSecond) / constant


       // System.out.println("Sissetulev metersPerSecond: " + metersPerSecond);
        double constant = 16.6666666666666666666667;

        double kmPerMinute = metersPerSecond / constant;
        //System.out.println("kilometersPerMinute: " +kmPerMinute);
        double resultForOneKilometer = 1 / kmPerMinute;
      //  System.out.println("Ühe km läbimise kiirus: " + resultForOneKilometer);

        int minutes = (int) resultForOneKilometer;
        int seconds = (int) ((resultForOneKilometer % 1) * 60);

       // System.out.println("Minutid: " + minutes);
        //System.out.println("Sekundid: "+seconds);
        String secondsStr = ""+seconds;
        if (seconds < 10) secondsStr = "0"+seconds;

        return minutes + ":" + secondsStr;
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
    protected void onResume(){
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");
        }

        if (locationManager!=null){
            locationManager.requestLocationUpdates(provider, 500, 1, this);
        }
    }


    @Override
    protected void onPause(){
        super.onPause();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No COARSE location permissions!");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No FINE location permissions!");
        }

        if (locationManager!=null){
            locationManager.removeUpdates(this);
        }
    }

}
