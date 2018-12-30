package org.tasks.preferences;

import android.app.Activity;
import androidx.core.app.ActivityCompat;
import javax.inject.Inject;

public class ActivityPermissionRequestor extends PermissionRequestor {

  private final Activity activity;

  @Inject
  public ActivityPermissionRequestor(Activity activity, PermissionChecker permissionChecker) {
    super(permissionChecker);

    this.activity = activity;
  }

  @Override
  protected void requestPermissions(String[] permissions, int rc) {
    ActivityCompat.requestPermissions(activity, permissions, rc);
  }
}
