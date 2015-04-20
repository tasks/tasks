package org.tasks.location;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.ForApplication;

import javax.inject.Inject;

public class GoogleApi implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static final Logger log = LoggerFactory.getLogger(GoogleApi.class);

    private GoogleApiClient.Builder builder;
    private GoogleApiClient googleApiClient;
    private GoogleApiClientConnectionHandler googleApiClientConnectionHandler;
    private boolean enableAutoManage;

    public interface GoogleApiClientConnectionHandler {
        void onConnect(GoogleApiClient client);
    }

    @Inject
    public GoogleApi(@ForApplication Context context) {
        builder = new GoogleApiClient.Builder(context, this, this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(this);
    }

    public void connect(final GoogleApiClientConnectionHandler googleApiClientConnectionHandler) {
        this.googleApiClientConnectionHandler = googleApiClientConnectionHandler;
        googleApiClient = builder.build();
        if (!enableAutoManage) {
            googleApiClient.connect();
        }
    }

    protected GoogleApi enableAutoManage(FragmentActivity fragmentActivity, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        enableAutoManage = true;
        builder.enableAutoManage(fragmentActivity, hashCode(), onConnectionFailedListener);
        return this;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        log.error("onConnectionFailed({})", connectionResult);
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
