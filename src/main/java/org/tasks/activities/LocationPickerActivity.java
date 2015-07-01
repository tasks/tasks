package org.tasks.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import org.tasks.dialogs.LocationPickerDialog;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.location.Geofence;
import org.tasks.location.OnLocationPickedHandler;
import org.tasks.preferences.ActivityPreferences;

import javax.inject.Inject;

public class LocationPickerActivity extends InjectingAppCompatActivity implements OnLocationPickedHandler, DialogInterface.OnCancelListener {

    private static final String FRAG_TAG_LOCATION_PICKER = "frag_tag_location_picker";

    public static final String EXTRA_GEOFENCE = "extra_geofence";

    @Inject ActivityPreferences activityPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityPreferences.applyDialogTheme();

        FragmentManager supportFragmentManager = getSupportFragmentManager();
        LocationPickerDialog dialog = (LocationPickerDialog) supportFragmentManager.findFragmentByTag(FRAG_TAG_LOCATION_PICKER);
        if (dialog == null) {
            dialog = new LocationPickerDialog();
            dialog.show(supportFragmentManager, FRAG_TAG_LOCATION_PICKER);
        }
        dialog.setOnCancelListener(this);
        dialog.setOnLocationPickedHandler(this);
    }

    @Override
    public void onLocationPicked(final Geofence geofence) {
        setResult(RESULT_OK, new Intent() {{
            putExtra(EXTRA_GEOFENCE, geofence);
        }});
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        setResult(RESULT_CANCELED);
        finish();
    }
}
