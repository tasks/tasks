package org.tasks.preferences;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastQ;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastR;

import android.Manifest.permission;

public abstract class PermissionRequestor {

  public static final int REQUEST_GOOGLE_ACCOUNTS = 53;
  public static final int REQUEST_BACKGROUND_LOCATION = 54;
  public static final int REQUEST_FOREGROUND_LOCATION = 55;

  private final PermissionChecker permissionChecker;

  PermissionRequestor(PermissionChecker permissionChecker) {
    this.permissionChecker = permissionChecker;
  }

  public boolean requestAccountPermissions() {
    if (permissionChecker.canAccessAccounts()) {
      return true;
    }
    requestPermissions(REQUEST_GOOGLE_ACCOUNTS, permission.GET_ACCOUNTS);
    return false;
  }

  public boolean requestForegroundLocation() {
    if (permissionChecker.canAccessForegroundLocation()) {
      return true;
    }
    requestPermissions(REQUEST_FOREGROUND_LOCATION, permission.ACCESS_FINE_LOCATION);
    return false;
  }

  public boolean requestBackgroundLocation() {
    if (permissionChecker.canAccessBackgroundLocation()) {
      return true;
    }
    if (atLeastR()) {
      if (requestForegroundLocation()) {
        requestPermissions(REQUEST_BACKGROUND_LOCATION, permission.ACCESS_BACKGROUND_LOCATION);
      }
    } else if (atLeastQ()) {
      requestPermissions(
          REQUEST_BACKGROUND_LOCATION,
          permission.ACCESS_FINE_LOCATION, permission.ACCESS_BACKGROUND_LOCATION);
    } else {
      requestPermissions(REQUEST_BACKGROUND_LOCATION, permission.ACCESS_FINE_LOCATION);
    }
    return false;
  }

  protected abstract void requestPermissions(int requestCode, String... permissions);
}
