package org.tasks.preferences;

import android.app.Activity;
import javax.inject.Inject;

public class ActivityPermissionRequestor extends PermissionRequestor {

  private final Activity activity;

  @Inject
  public ActivityPermissionRequestor(Activity activity, PermissionChecker permissionChecker) {
    super(permissionChecker);

    this.activity = activity;
  }

  @Override
  protected void requestPermissions(int rc, String... permissions) {
    activity.requestPermissions(permissions, rc);
  }
}
