package org.tasks.preferences;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastQ;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

public class PermissionChecker {

  private final Context context;

  @Inject
  public PermissionChecker(@ApplicationContext Context context) {
    this.context = context;
  }

  public boolean canAccessCalendars() {
    return checkPermissions(permission.READ_CALENDAR, permission.WRITE_CALENDAR);
  }

  public boolean canAccessAccounts() {
    return atLeastOreo() || checkPermissions(permission.GET_ACCOUNTS);
  }

  public boolean canAccessForegroundLocation() {
    return checkPermissions(permission.ACCESS_FINE_LOCATION);
  }

  public boolean canAccessBackgroundLocation() {
    return atLeastQ()
        ? canAccessForegroundLocation() && checkPermissions(permission.ACCESS_BACKGROUND_LOCATION)
        : canAccessForegroundLocation();
  }

  private boolean checkPermissions(String... permissions) {
    for (String permission : permissions) {
      if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
        Timber.w("Request for %s denied", permission);
        return false;
      }
    }
    return true;
  }
}
