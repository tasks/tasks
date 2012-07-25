/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.taskrabbit;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

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
    public Location currentLocation;
    private LocationManager locationManager;
    private EditText searchText;
    private TaskRabbitMapOverlayItem currentOverlayItem;
    private String locationAddress;
    private static final int LOCATION_SEARCH_SUCCESS = 1;
    private static final int LOCATION_SEARCH_FAIL = -1;

    // Production value
    private static final String MAPS_API_KEY = "0J-miH1uUbgVV5xsNNmvSIzb4DIENVCMERxB7gw"; //$NON-NLS-1$

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_rabbit_map_activity);

        mapView = new MapView(this, MAPS_API_KEY);
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(true);

        LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 12.0f);

        ((LinearLayout) findViewById(R.id.task_rabbit_map_parent)).addView(mapView, 0, lp);


        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        mapController = mapView.getController();
        if(currentLocation != null) {

            updateLocationOverlay();

            locationAddress = getAddressFromLocation(currentLocation);
            mapController.setZoom(17);
            mapView.invalidate();

        }
        else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        }


        searchText=(EditText)findViewById(R.id.search_text);

        ImageButton searchButton=(ImageButton)findViewById(R.id.search_button);
        searchButton.setImageResource(android.R.drawable.ic_menu_search);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchLocation();
            }
        });


    }


    public String getSearchText() {
        return searchText.getText().toString();
    }
    public void setSearchTextForCurrentAddress() {
        if(!TextUtils.isEmpty(locationAddress)) {
            searchText.setText(locationAddress);
        }
    }


    private final Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case LOCATION_SEARCH_SUCCESS:

                mapView.invalidate();
                currentOverlayItem.onTap(0);
                // What to do when ready, example:
                break;
            case LOCATION_SEARCH_FAIL:

                AlertDialog.Builder adb = new AlertDialog.Builder(TaskRabbitMapActivity.this);
                adb.setTitle(getString(R.string.tr_alert_location_fail_title));
                adb.setMessage(getString(R.string.tr_alert_location_fail_message));
                adb.setPositiveButton(R.string.DLG_close, null);
                adb.show();
            }
        }
    };

    private void searchLocation() {

        Thread thread = new Thread() {
            @Override
            public void run() {

                List<Address> addresses = null;
                try {

                    Geocoder geoCoder = new Geocoder(
                            TaskRabbitMapActivity.this, Locale.getDefault());
                    addresses = geoCoder.getFromLocationName(
                            searchText.getText().toString(), 5);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (addresses != null && addresses.size() > 0) {
                    updateAddress(addresses.get(0));
                    currentLocation = new Location("");
                    currentLocation.setLatitude(addresses.get(0).getLatitude());
                    currentLocation.setLongitude(addresses.get(0).getLongitude());
                    mapController.setZoom(12);
                    updateLocationOverlay();


                    Message successMessage = new Message();
                    successMessage.what = LOCATION_SEARCH_SUCCESS;
                    handler.sendMessage(successMessage);
                } else {

                    Message failureMessage = new Message();
                    failureMessage.what = LOCATION_SEARCH_FAIL;
                    handler.sendMessage(failureMessage);

                }
            }

        };

        thread.start();

    }

    protected OverlayItem createOverlayItem(GeoPoint q) {
        OverlayItem overlayitem = new OverlayItem(q, getString(R.string.tr_alert_location_clicked_title),
                getString(R.string.tr_alert_location_clicked_message));
        return overlayitem;
    }

    public String getAddressFromLocation(Location location){
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            // Acquire a reference to the system Location Manager
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null){
                for (Address address : addresses){
                    return updateAddress(address);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ""; //$NON-NLS-1$
    }
    private String updateAddress(Address address){
        String addressString = null;
        if(address.getLocality() != null && address.getPostalCode() != null){
            addressString = "";  //$NON-NLS-1$
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++){
                addressString += address.getAddressLine(i) + ", ";  //$NON-NLS-1$
            }
        }
        return addressString;
    }


    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private void updateLocationOverlay() {
        if (currentLocation == null) { return; };
        List<Overlay> mapOverlays = mapView.getOverlays();
        Drawable drawable = this.getResources().getDrawable(android.R.drawable.star_big_on);
        currentOverlayItem = new TaskRabbitMapOverlayItem(drawable, this);
        GeoPoint point = locationToGeoPoint(currentLocation);
        OverlayItem overlayitem = createOverlayItem(point);
        currentOverlayItem.addOverlay(overlayitem);
        mapOverlays.clear();
        mapOverlays.add(currentOverlayItem);
        mapController.animateTo(point);
    }

    public void didSelectItem (final OverlayItem selectedItem) {

        AlertDialog.Builder dialogPrompt = new AlertDialog.Builder(this);
        dialogPrompt.setTitle(getString(R.string.tr_alert_location_clicked_title));
        Location location = geoPointToLocation(selectedItem.getPoint());
        locationAddress = getAddressFromLocation(location);
        setSearchTextForCurrentAddress();
        dialogPrompt.setMessage(locationAddress);
        dialogPrompt.setIcon(
                android.R.drawable.ic_dialog_alert).setPositiveButton(
                        android.R.string.ok, new DialogInterface.OnClickListener() {
                            @SuppressWarnings("nls")
                            public void onClick(DialogInterface dialog, int which) {
                                Intent data = new Intent();
                                data.putExtra("lat",selectedItem.getPoint().getLatitudeE6());
                                data.putExtra("lng",selectedItem.getPoint().getLongitudeE6());
                                data.putExtra("name", getSearchText());
                                setResult(Activity.RESULT_OK, data);
                                finish();
                            }
                        }).setNegativeButton(android.R.string.cancel, null);
        dialogPrompt.show();
    }

    private Location geoPointToLocation(GeoPoint geoPoint) {
        Location location = new Location(""); //$NON-NLS-1$
        location.setLatitude(((long)geoPoint.getLatitudeE6())/1E6);
        location.setLongitude(((long)geoPoint.getLongitudeE6())/1E6);
        return location;
    }

    private GeoPoint locationToGeoPoint(Location lastKnownLocation) {
        GeoPoint point = new GeoPoint((int)(lastKnownLocation.getLatitude()*1E6),(int)(lastKnownLocation.getLongitude()*1E6));
        return point;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            locationManager.removeUpdates(this);
            this.currentLocation = location;
            updateLocationOverlay();

        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        //

    }
    @Override
    public void onProviderEnabled(String provider) {
        //

    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //

    }



}

