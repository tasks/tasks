package org.tasks.preferences;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo;
import static com.todoroo.andlib.utility.AndroidUtilities.preJellybean;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import java.util.List;
import javax.inject.Inject;
import org.tasks.injection.ForApplication;
import timber.log.Timber;

public class PermissionChecker {

  private final Context context;

  @Inject
  public PermissionChecker(@ForApplication Context context) {
    this.context = context;
  }

  public boolean canAccessCalendars() {
    return checkPermissions(
        asList(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR));
  }

  public boolean canWriteToExternalStorage() {
    return checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
  }

  public boolean canAccessAccounts() {
    return atLeastOreo() || checkPermission(Manifest.permission.GET_ACCOUNTS);
  }

  public boolean canAccessLocation() {
    return checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
  }

  public boolean canAccessMic() {
    return checkPermission(Manifest.permission.RECORD_AUDIO);
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  public boolean canAccessMissedCallPermissions() {
    return checkPermission(Manifest.permission.READ_CONTACTS)
        && checkPermission(Manifest.permission.READ_PHONE_STATE)
        && (preJellybean() || checkPermission(Manifest.permission.READ_CALL_LOG));
  }

  private boolean checkPermission(String permission) {
    return checkPermissions(singletonList(permission));
  }

  private boolean checkPermissions(List<String> permissions) {
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
