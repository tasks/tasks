package org.tasks.preferences;

import android.support.v4.app.Fragment;
import javax.inject.Inject;

public class FragmentPermissionRequestor extends PermissionRequestor {

  private final Fragment fragment;

  @Inject
  public FragmentPermissionRequestor(Fragment fragment, PermissionChecker permissionChecker) {
    super(permissionChecker);

    this.fragment = fragment;
  }

  @Override
  protected void requestPermissions(String[] permissions, int rc) {
    fragment.requestPermissions(permissions, rc);
  }
}
