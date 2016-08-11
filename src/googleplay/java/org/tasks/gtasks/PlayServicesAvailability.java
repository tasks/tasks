package org.tasks.gtasks;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import timber.log.Timber;

public class PlayServicesAvailability {

    private static final int REQUEST_RESOLUTION = 10000;

    private final Context context;
    private final Preferences preferences;

    @Inject
    public PlayServicesAvailability(@ForApplication Context context, Preferences preferences) {
        this.context = context;
        this.preferences = preferences;
    }

    public boolean refreshAndCheck() {
        refresh();
        return isPlayServicesAvailable();
    }

    public boolean isPlayServicesAvailable() {
        return getResult() == ConnectionResult.SUCCESS;
    }

    public void refresh() {
        GoogleApiAvailability instance = GoogleApiAvailability.getInstance();
        int googlePlayServicesAvailable = instance.isGooglePlayServicesAvailable(context);
        preferences.setInt(R.string.play_services_available, googlePlayServicesAvailable);
        if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
            preferences.setBoolean(R.string.warned_play_services, false);
        }
        Timber.d(getStatus());
    }

    public void resolve(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int error = preferences.getInt(R.string.play_services_available, -1);
        if (googleApiAvailability.isUserResolvableError(error)) {
            googleApiAvailability.getErrorDialog(activity, error, REQUEST_RESOLUTION).show();
        } else {
            Toast.makeText(activity, R.string.common_google_play_services_notification_ticker, Toast.LENGTH_LONG).show();
        }
    }

    public String getStatus() {
        return GoogleApiAvailability.getInstance().getErrorString(getResult());
    }

    private int getResult() {
        return preferences.getInt(R.string.play_services_available, -1);
    }
}
