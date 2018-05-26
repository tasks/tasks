package org.tasks;

import android.content.pm.PackageManager;

public abstract class PermissionUtil {

  public static boolean verifyPermissions(int[] grantResults) {
    if (grantResults.length == 0) {
      // request canceled
      return false;
    }

    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }
}
