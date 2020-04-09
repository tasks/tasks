package org.tasks.preferences;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastQ;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import javax.inject.Inject;
import org.tasks.injection.ForApplication;
import timber.log.Timber;

public class PermissionChecker {

  private final Context context;

  @Inject
  PermissionChecker(@ForApplication Context context) {
    this.context = context;
  }

  public boolean canAccessCalendars() {
    return checkPermissions(permission.READ_CALENDAR, permission.WRITE_CALENDAR);
  }

  public boolean canAccessAccounts() {
    return atLeastOreo() || checkPermissions(permission.GET_ACCOUNTS);
  }

  public boolean canAccessLocation() {
    return atLeastQ()
        ? checkPermissions(permission.ACCESS_FINE_LOCATION, permission.ACCESS_BACKGROUND_LOCATION)
        : checkPermissions(permission.ACCESS_FINE_LOCATION);
  }

  public boolean canAccessMic() {
    return checkPermissions(permission.RECORD_AUDIO);
  }

  private boolean checkPermissions(String... permissions) {
    for (String permission : permissions) {
      if (ActivityCompat.checkSelfPermission(context, permission)
          != PackageManager.PERMISSION_GRANTED) {
        Timber.w("Request for %s denied", permission);
        return false;
      }
    }
    return true;
  }
}
