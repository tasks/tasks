package org.tasks.preferences;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import org.tasks.injection.ForApplication;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static java.util.Arrays.asList;

public class PermissionChecker {

    private final Context context;

    @Inject
    public PermissionChecker(@ForApplication Context context) {
        this.context = context;
    }

    public boolean canAccessCalendars() {
        return checkPermissions(asList(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR));
    }

    public boolean canWriteToExternalStorage() {
        return checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public boolean canAccessAccounts() {
        return checkPermission(Manifest.permission.GET_ACCOUNTS);
    }

    public boolean canAccessLocation() {
        return checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public boolean canAccessMic() {
        return checkPermission(Manifest.permission.RECORD_AUDIO);
    }

    public boolean canAccessMissedCallPermissions() {
        return checkPermission(Manifest.permission.READ_CONTACTS) &&
                checkPermission(Manifest.permission.READ_PHONE_STATE);
    }

    private boolean checkPermission(String permission) {
        return checkPermissions(asList(permission));
    }

    private boolean checkPermissions(List<String> permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                Timber.w("Request for %s denied", permission);
                return false;
            }
        }
        return true;
    }
}
