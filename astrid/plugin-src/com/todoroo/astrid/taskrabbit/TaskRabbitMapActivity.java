package com.todoroo.astrid.taskrabbit;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.timsu.astrid.R;


public class TaskRabbitMapActivity extends MapActivity implements LocationListener {

    private MapView mapView;
    private MapController mapController;
    public Location location;
    private EditText searchText;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_rabbit_map_activity);

        mapView = (MapView) findViewById(R.id.map_view);
        mapView.setBuiltInZoomControls(true);


        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 500.0f, this);

        List<Overlay> mapOverlays = mapView.getOverlays();

        Drawable drawable = this.getResources().getDrawable(R.drawable.icon_locale);
        TaskRabbitMapOverlayItem itemizedoverlay = new TaskRabbitMapOverlayItem(drawable, this);
        GeoPoint point = null;
        if(lastKnownLocation != null) {

            point = new GeoPoint((int)(lastKnownLocation.getLatitude()*1E6),(int)(lastKnownLocation.getLongitude()*1E6));
        }
        else {
            point = new GeoPoint(19240000,-99120000);
        }

        OverlayItem overlayitem = new OverlayItem(point, "Set this location", "For the rabbits!");
        itemizedoverlay.addOverlay(overlayitem);
        mapOverlays.add(itemizedoverlay);




        mapController = mapView.getController();
        mapController.animateTo(point);
        mapController.setZoom(17);
        mapView.invalidate();

        searchText=(EditText)findViewById(R.id.search_text);

        Button searchButton=(Button)findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                searchLocation();
            }
        });

    }

    private final Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 1:

                mapView.invalidate();
                // What to do when ready, example:
                break;
            case -1:

                AlertDialog.Builder adb = new AlertDialog.Builder(TaskRabbitMapActivity.this);
                adb.setTitle("Google Map");
                adb.setMessage("Please Provide the Proper Place");
                adb.setPositiveButton("Close",null);
                adb.show();
            }
        }
    };

    private void searchLocation() {

                Thread thread = new Thread(){
                    @Override
                    public void run (){

        List<Address> addresses = null;
        try {

            Geocoder geoCoder = new Geocoder(TaskRabbitMapActivity.this, Locale.getDefault());
            addresses = geoCoder.getFromLocationName(searchText.getText().toString(),5);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if(addresses != null && addresses.size() > 0)
        {
            GeoPoint q = new GeoPoint( (int) (addresses.get(0).getLatitude() * 1E6),
                    (int) (addresses.get(0).getLongitude() * 1E6));

            Drawable drawable = TaskRabbitMapActivity.this.getResources().getDrawable(R.drawable.icon_locale);
            TaskRabbitMapOverlayItem itemizedoverlay = new TaskRabbitMapOverlayItem(drawable, TaskRabbitMapActivity.this);
            mapController.animateTo(q);
            mapController.setZoom(12);

            OverlayItem overlayitem = new OverlayItem(q, "Set this location", "For the rabbits!");

            itemizedoverlay.addOverlay(overlayitem);
            List<Overlay> mapOverlays = mapView.getOverlays();
            mapOverlays.clear();
            mapOverlays.add(itemizedoverlay);

            Message successMessage = new Message();
            successMessage.what = 1;
            handler.sendMessage(successMessage);
            //                       searchText.setText("");
        }
        else
        {

            Message failureMessage = new Message();
            failureMessage.what = -1;
            handler.sendMessage(failureMessage);

        }
                    }
                };

                thread.start();

    }
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private void updateLocationOverlay() {
        if (location == null) { return; };
        List<Overlay> mapOverlays = mapView.getOverlays();
        Drawable drawable = this.getResources().getDrawable(R.drawable.gl_alarm);
        TaskRabbitMapOverlayItem myItemizedOverlay = new TaskRabbitMapOverlayItem(drawable);
        GeoPoint point = new GeoPoint((int)(location.getLatitude() * 1E6), (int)(location.getLongitude() * 1E6));

        OverlayItem overlayitem = new OverlayItem(point, "Astrid!!", "WOrks!");
        myItemizedOverlay.addOverlay(overlayitem);
        mapOverlays.add(myItemizedOverlay);
    }
    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub
        if (location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            this.location = location;
            GeoPoint p = new GeoPoint((int) lat * 1000000, (int) lng * 1000000);
            mapController.animateTo(p);
        }
    }
    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }
    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }



}

