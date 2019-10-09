package org.tasks.preferences;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastQ;

import android.Manifest.permission;

public abstract class PermissionRequestor {

  public static final int REQUEST_CALENDAR = 51;
  public static final int REQUEST_MIC = 52;
  public static final int REQUEST_GOOGLE_ACCOUNTS = 53;
  public static final int REQUEST_LOCATION = 54;

  private final PermissionChecker permissionChecker;

  PermissionRequestor(PermissionChecker permissionChecker) {
    this.permissionChecker = permissionChecker;
  }

  public void requestMic() {
    if (!permissionChecker.canAccessMic()) {
      requestPermissions(REQUEST_MIC, permission.RECORD_AUDIO);
    }
  }

  public boolean requestCalendarPermissions() {
    return requestCalendarPermissions(REQUEST_CALENDAR);
  }

  public boolean requestCalendarPermissions(int requestCode) {
    if (permissionChecker.canAccessCalendars()) {
      return true;
    }
    requestPermissions(requestCode, permission.READ_CALENDAR, permission.WRITE_CALENDAR);
    return false;
  }

  public boolean requestAccountPermissions() {
    if (permissionChecker.canAccessAccounts()) {
      return true;
    }
    requestPermissions(REQUEST_GOOGLE_ACCOUNTS, permission.GET_ACCOUNTS);
    return false;
  }

  public boolean requestFineLocation() {
    if (permissionChecker.canAccessLocation()) {
      return true;
    }
    if (atLeastQ()) {
      requestPermissions(
          REQUEST_LOCATION, permission.ACCESS_FINE_LOCATION, permission.ACCESS_BACKGROUND_LOCATION);
    } else {
      requestPermissions(REQUEST_LOCATION, permission.ACCESS_FINE_LOCATION);
    }
    return false;
  }

  protected abstract void requestPermissions(int requestCode, String... permissions);
}
