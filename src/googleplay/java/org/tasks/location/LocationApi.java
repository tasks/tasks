package org.tasks.location;

import android.location.Location;

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

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class LocationApi {

    private static final Logger log = LoggerFactory.getLogger(LocationApi.class);

    private GoogleApiClientProvider googleApiClientProvider;

    @Inject
    public LocationApi(GoogleApiClientProvider googleApiClientProvider) {
        this.googleApiClientProvider = googleApiClientProvider;
    }

    public void getPlaceDetails(final String placeId, final ResultCallback<PlaceBuffer> callback) {
        googleApiClientProvider.getApi(new GoogleApiClientProvider.withApi() {
            @Override
            public void doWork(final GoogleApiClient googleApiClient) {
                Places.GeoDataApi.getPlaceById(googleApiClient, placeId)
                        .setResultCallback(new ResultCallback<PlaceBuffer>() {
                            @Override
                            public void onResult(PlaceBuffer places) {
                                callback.onResult(places);
                                googleApiClient.disconnect();
                            }
                        }, 15, TimeUnit.SECONDS);
            }

            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                log.error("onConnectionFailed({})", connectionResult);
            }
        });
    }

    public void getAutocompletePredictions(final String constraint, final ResultCallback<AutocompletePredictionBuffer> callback) {
        googleApiClientProvider.getApi(new GoogleApiClientProvider.withApi() {
            @Override
            public void doWork(final GoogleApiClient googleApiClient) {
                final LatLngBounds bounds = LatLngBounds.builder().include(getLastKnownLocation(googleApiClient)).build();
                Places.GeoDataApi.getAutocompletePredictions(googleApiClient, constraint, bounds, null)
                        .setResultCallback(new ResultCallback<AutocompletePredictionBuffer>() {
                            @Override
                            public void onResult(AutocompletePredictionBuffer autocompletePredictions) {
                                callback.onResult(autocompletePredictions);
                                googleApiClient.disconnect();
                            }
                        }, 15, TimeUnit.SECONDS);
            }

            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                log.error("onConnectionFailed({})", connectionResult);
            }
        });
    }

    private LatLng getLastKnownLocation(GoogleApiClient googleApiClient) {
        try {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            return new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        } catch (Exception e) {
            return new LatLng(0, 0);
        }
    }
}
