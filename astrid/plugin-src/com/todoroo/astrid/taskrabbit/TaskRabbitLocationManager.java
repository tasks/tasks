package com.todoroo.astrid.taskrabbit;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.timsu.astrid.data.location.GeoPoint;

public class TaskRabbitLocationManager {
    Timer timer1;
    LocationManager lm;
    LocationResult locationResult;
    boolean network_enabled=false;
    Context context;

    private static final int RADIUS_250_MILES = 400000;

    public TaskRabbitLocationManager(Context context) {
        this.context = context;
        lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

    }
    private static final GeoPoint[] supportedLocations = {
        new GeoPoint(42358430, -71059770), //BOS
        new GeoPoint(37739230, -122439880), //SF
        new GeoPoint(40714350, -74005970), //NYC
        new GeoPoint(41878110, -87629800), //CHI
        new GeoPoint(34052230, -118243680), //LA
        new GeoPoint(33717470, -117831140), //OC
        new GeoPoint(30267150, -97743060), //AUSTIN
        new GeoPoint(45523450, -122676210), //PORTLAND
        new GeoPoint(47606210, -122332070), //SEA
        new GeoPoint(29424120, -98493630) //SAN ANTONIO
    };

    public boolean isLocationUpdatesEnabled() {
        boolean provider_enabled = false;
        try {
            provider_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            // suppress
        }
        return provider_enabled;
    }
    public Location getLastKnownLocation()
    {
        if(!isNetworkProviderEnabled())
            return null;
        else {
            return lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }

    public static boolean supportsCurrentLocation(Location location) {
        if (location == null) return false;
        for (GeoPoint point : supportedLocations){
            Location city = new Location("");  //$NON-NLS-1$
            city.setLatitude(point.getLatitudeE6()/1E6);
            city.setLongitude(point.getLongitudeE6()/1E6);
            float distance = location.distanceTo(city);
            if (distance < RADIUS_250_MILES) {
                return true;
            }
        }
        return false;
    }



    public boolean getLocation(LocationResult result)
    {
        locationResult = result;

        if(!isNetworkProviderEnabled())
            return false;

        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        timer1=new Timer();
        timer1.schedule(new GetLastLocation(), 20000);
        return true;
    }

    private boolean isNetworkProviderEnabled() {
        try {
            return lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            // suppress
        }
        return false;
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            timer1.cancel();
            locationResult.gotLocation(location);
            lm.removeUpdates(locationListener);
        }
        public void onProviderDisabled(String provider) { /**/ }
        public void onProviderEnabled(String provider) { /**/ }
        public void onStatusChanged(String provider, int status, Bundle extras) { /**/ }
    };

    class GetLastLocation extends TimerTask {
        @Override
        public void run() {
            lm.removeUpdates(locationListener);

            Location net_loc=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if(net_loc!=null){
                locationResult.gotLocation(net_loc);
                return;
            }
            locationResult.gotLocation(null);
        }
    }

    public static abstract class LocationResult{
        public abstract void gotLocation(Location location);
    }
}
