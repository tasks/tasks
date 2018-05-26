package org.tasks.gtasks;

import android.app.Activity;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import javax.inject.Inject;

public class PlayServices {

  @Inject
  public PlayServices() {}

  public boolean isPlayServicesAvailable() {
    return false;
  }

  public boolean refreshAndCheck() {
    return false;
  }

  public void resolve(Activity activity) {}

  public String getStatus() {
    return null;
  }

  public boolean clearToken(GoogleAccountCredential googleAccountCredential) {
    return false;
  }

  public void getAuthToken(
      GtasksLoginActivity gtasksLoginActivity,
      String a,
      GtasksLoginActivity.AuthResultHandler authResultHandler) {}
}
