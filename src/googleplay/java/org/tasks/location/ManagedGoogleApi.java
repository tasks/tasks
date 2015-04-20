package org.tasks.location;

import android.content.IntentSender;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

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
import org.tasks.R;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ManagedGoogleApi extends GoogleApi implements GoogleApi.GoogleApiClientConnectionHandler {
    private static final int RC_RESOLVE_GPS_ISSUE = 10009;

    private static final Logger log = LoggerFactory.getLogger(ManagedGoogleApi.class);
    private FragmentActivity fragmentActivity;
    private GoogleApiClient googleApiClient;

    @Inject
    public ManagedGoogleApi(FragmentActivity fragmentActivity) {
        super(fragmentActivity);

        this.fragmentActivity = fragmentActivity;
        enableAutoManage(fragmentActivity, this);
    }

    public void connect() {
        if (googleApiClient == null) {
            super.connect(this);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(fragmentActivity, RC_RESOLVE_GPS_ISSUE);
            } catch (IntentSender.SendIntentException e) {
                log.error(e.getMessage(), e);
            }
        } else {
            Toast.makeText(fragmentActivity, String.format("%s: %s\n%s",
                    fragmentActivity.getString(R.string.app_name),
                    fragmentActivity.getString(R.string.common_google_play_services_notification_ticker),
                    connectionResult.getErrorCode()), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnect(GoogleApiClient googleApiClient) {
        this.googleApiClient = googleApiClient;
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
}
