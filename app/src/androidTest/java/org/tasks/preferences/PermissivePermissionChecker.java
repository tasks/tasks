package org.tasks.preferences;

import android.content.Context;
import org.tasks.injection.ForApplication;

public class PermissivePermissionChecker extends PermissionChecker {

  public PermissivePermissionChecker(@ForApplication Context context) {
    super(context);
  }

  @Override
  public boolean canAccessCalendars() {
    return true;
  }

  @Override
  public boolean canWriteToExternalStorage() {
    return true;
  }

  @Override
  public boolean canAccessAccounts() {
    return true;
  }

  @Override
  public boolean canAccessLocation() {
    return true;
  }

  @Override
  public boolean canAccessMic() {
    return true;
  }

  @Override
  public boolean canAccessMissedCallPermissions() {
    return true;
  }
}
