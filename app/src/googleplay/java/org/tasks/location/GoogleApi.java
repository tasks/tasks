package org.tasks.location;

import android.content.Context;
import android.os.Bundle;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import javax.inject.Inject;
import org.tasks.injection.ForApplication;
import timber.log.Timber;

public class GoogleApi implements GoogleApiClient.ConnectionCallbacks {

  private final GoogleApiClient.Builder builder;
  private GoogleApiClient googleApiClient;
  private GoogleApiClientConnectionHandler googleApiClientConnectionHandler;

  @Inject
  public GoogleApi(@ForApplication Context context) {
    builder =
        new GoogleApiClient.Builder(context)
            .addApi(LocationServices.API)
            .addApi(Places.GEO_DATA_API)
            .addConnectionCallbacks(this);
  }

  public void connect(final GoogleApiClientConnectionHandler googleApiClientConnectionHandler) {
    connect(
        googleApiClientConnectionHandler,
        connectionResult -> Timber.e("onConnectionFailed(%s)", connectionResult));
  }

  private void connect(
      final GoogleApiClientConnectionHandler googleApiClientConnectionHandler,
      GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
    this.googleApiClientConnectionHandler = googleApiClientConnectionHandler;
    googleApiClient = builder.addOnConnectionFailedListener(onConnectionFailedListener).build();
    googleApiClient.connect();
  }

  @Override
  public void onConnected(Bundle bundle) {
    Timber.i("onConnected(Bundle)");
    googleApiClientConnectionHandler.onConnect(googleApiClient);
  }

  @Override
  public void onConnectionSuspended(int i) {
    Timber.i("onConnectionSuspended(%s)", i);
  }

  public interface GoogleApiClientConnectionHandler {

    void onConnect(GoogleApiClient client);
  }
}
