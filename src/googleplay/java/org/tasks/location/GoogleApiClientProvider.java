package org.tasks.location;

import android.content.Context;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.ForApplication;

import javax.inject.Inject;

public class GoogleApiClientProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleApiClientProvider.class);

    private Context context;

    public interface withApi extends GoogleApiClient.OnConnectionFailedListener {
        void doWork(GoogleApiClient googleApiClient);
    }

    @Inject
    public GoogleApiClientProvider(@ForApplication Context context) {
        this.context = context;
    }

    public void getApi(final withApi callback) {
        final GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addOnConnectionFailedListener(callback)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                log.info("onConnected({})", bundle);
                callback.doWork(googleApiClient);
            }

            @Override
            public void onConnectionSuspended(int i) {
                log.info("onConnectionSuspended({})", i);
            }
        });
        googleApiClient.connect();
    }
}
