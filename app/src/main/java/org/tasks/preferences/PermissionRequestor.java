package org.tasks.preferences;

import android.Manifest;

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
      requestPermission(Manifest.permission.RECORD_AUDIO, REQUEST_MIC);
    }
  }

  public boolean requestCalendarPermissions() {
    return requestCalendarPermissions(REQUEST_CALENDAR);
  }

  public boolean requestCalendarPermissions(int requestCode) {
    if (permissionChecker.canAccessCalendars()) {
      return true;
    }
    requestPermissions(
        new String[] {Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR},
        requestCode);
    return false;
  }

  public boolean requestAccountPermissions() {
    if (permissionChecker.canAccessAccounts()) {
      return true;
    }
    requestPermission(Manifest.permission.GET_ACCOUNTS, REQUEST_GOOGLE_ACCOUNTS);
    return false;
  }

  public boolean requestFineLocation() {
    if (permissionChecker.canAccessLocation()) {
      return true;
    }
    requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_LOCATION);
    return false;
  }

  private void requestPermission(String permission, int rc) {
    requestPermissions(new String[] {permission}, rc);
  }

  protected abstract void requestPermissions(String[] permissions, int requestCode);
}
