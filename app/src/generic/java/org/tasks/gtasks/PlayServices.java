package org.tasks.gtasks;

import android.app.Activity;
import com.todoroo.astrid.activity.MainActivity;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
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

  public Disposable check(MainActivity mainActivity) {
    return Disposables.empty();
  }
}
