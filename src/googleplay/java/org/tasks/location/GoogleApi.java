package org.tasks.location;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.ForApplication;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class GoogleApi implements GoogleApiClient.ConnectionCallbacks {

    private static final Logger log = LoggerFactory.getLogger(GoogleApi.class);

    private GoogleApiClient.Builder builder;
    private GoogleApiClient googleApiClient;
    private GoogleApiClientConnectionHandler googleApiClientConnectionHandler;

    public interface GoogleApiClientConnectionHandler {
        void onConnect(GoogleApiClient client);
    }

    @Inject
    public GoogleApi(@ForApplication Context context) {
        builder = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(this);
    }

    public void getPlaceDetails(final String placeId, final ResultCallback<PlaceBuffer> callback) {
        Places.GeoDataApi.getPlaceById(googleApiClient, placeId).setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(PlaceBuffer places) {
                callback.onResult(places);
            }
        });
    }

    public void getAutocompletePredictions(final String constraint, final ResultCallback<AutocompletePredictionBuffer> callback) {
        final LatLngBounds bounds = LatLngBounds.builder().include(getLastKnownLocation(googleApiClient)).build();
        Places.GeoDataApi.getAutocompletePredictions(googleApiClient, constraint, bounds, null)
                .setResultCallback(new ResultCallback<AutocompletePredictionBuffer>() {
                    @Override
                    public void onResult(AutocompletePredictionBuffer autocompletePredictions) {
                        callback.onResult(autocompletePredictions);
                    }
                }, 15, TimeUnit.SECONDS);
    }

    private LatLng getLastKnownLocation(GoogleApiClient googleApiClient) {
        try {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            return new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new LatLng(0, 0);
        }
    }

    public void connect(final GoogleApiClientConnectionHandler googleApiClientConnectionHandler) {
        connect(googleApiClientConnectionHandler, new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                log.error("onConnectionFailed({})", connectionResult);
            }
        });
    }

    public void connect(final GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        connect(new GoogleApiClientConnectionHandler() {
            @Override
            public void onConnect(GoogleApiClient client) {
                log.info("onConnect({})", client);
            }
        }, onConnectionFailedListener);
    }

    private void connect(final GoogleApiClientConnectionHandler googleApiClientConnectionHandler, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        this.googleApiClientConnectionHandler = googleApiClientConnectionHandler;
        googleApiClient = builder
                .addOnConnectionFailedListener(onConnectionFailedListener)
                .build();
        googleApiClient.connect();
    }

    public void disconnect() {
        googleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        log.info("onConnected(Bundle)");
        googleApiClientConnectionHandler.onConnect(googleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        log.info("onConnectionSuspended({})", i);
    }
}
