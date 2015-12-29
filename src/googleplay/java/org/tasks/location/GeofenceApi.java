package org.tasks.location;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.common.base.Function;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;

public class GeofenceApi {

    private Context context;
    private Preferences preferences;
    private PermissionChecker permissionChecker;

    @Inject
    public GeofenceApi(@ForApplication Context context, Preferences preferences, PermissionChecker permissionChecker) {
        this.context = context;
        this.preferences = preferences;
        this.permissionChecker = permissionChecker;
    }

    public void register(final List<Geofence> geofences) {
        if (geofences.isEmpty() || !preferences.geofencesEnabled() || !permissionChecker.canAccessLocation()) {
            return;
        }

        newClient(new GoogleApi.GoogleApiClientConnectionHandler() {
            @Override
            public void onConnect(final GoogleApiClient client) {
                @SuppressWarnings("ResourceType")
                @SuppressLint("MissingPermission")
                PendingResult<Status> result = LocationServices.GeofencingApi.addGeofences(
                        client,
                        getRequests(geofences),
                        PendingIntent.getService(context, 0, new Intent(context, GeofenceTransitionsIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT));
                result.setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Timber.i("Registered %s", geofences);
                        } else {
                            Timber.e("Failed to register %s", geofences);
                        }

                        client.disconnect();
                    }
                });
            }
        });
    }

    public void cancel(final Geofence geofence) {
        cancel(singletonList(geofence));
    }

    public void cancel(final List<Geofence> geofences) {
        if (geofences.isEmpty() || !permissionChecker.canAccessLocation()) {
            return;
        }

        final List<String> ids = newArrayList(transform(geofences, new Function<Geofence, String>() {
            @Override
            public String apply(Geofence geofence) {
                return Long.toString(geofence.getMetadataId());
            }
        }));

        newClient(new GoogleApi.GoogleApiClientConnectionHandler() {
            @Override
            public void onConnect(final GoogleApiClient client) {
                LocationServices.GeofencingApi.removeGeofences(client, ids)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    Timber.i("Removed %s", geofences);
                                } else {
                                    Timber.e("Failed to remove %s", geofences);
                                }

                                client.disconnect();
                            }
                        });
            }
        });
    }

    private void newClient(final GoogleApi.GoogleApiClientConnectionHandler handler) {
        new GoogleApi(context).connect(handler);
    }

    private List<com.google.android.gms.location.Geofence> getRequests(List<Geofence> geofences) {
        return newArrayList(transform(geofences, new Function<Geofence, com.google.android.gms.location.Geofence>() {
            @Override
            public com.google.android.gms.location.Geofence apply(Geofence geofence) {
                return toGoogleGeofence(geofence);
            }
        }));
    }

    private com.google.android.gms.location.Geofence toGoogleGeofence(Geofence geofence) {
        int radius = preferences.getIntegerFromString(R.string.p_geofence_radius, 250);
        int responsiveness = (int) TimeUnit.SECONDS.toMillis(preferences.getIntegerFromString(R.string.p_geofence_responsiveness, 60));
        return new com.google.android.gms.location.Geofence.Builder()
                .setCircularRegion(geofence.getLatitude(), geofence.getLongitude(), radius)
                .setNotificationResponsiveness(responsiveness)
                .setRequestId(Long.toString(geofence.getMetadataId()))
                .setTransitionTypes(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .setExpirationDuration(NEVER_EXPIRE)
                .build();
    }
}
