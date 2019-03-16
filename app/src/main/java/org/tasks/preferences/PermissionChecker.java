package org.tasks.preferences;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastOreo;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
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

  public boolean canAccessAccounts() {
    return atLeastOreo() || checkPermission(Manifest.permission.GET_ACCOUNTS);
  }

  public boolean canAccessLocation() {
    return checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
  }

  public boolean canAccessMic() {
    return checkPermission(Manifest.permission.RECORD_AUDIO);
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
