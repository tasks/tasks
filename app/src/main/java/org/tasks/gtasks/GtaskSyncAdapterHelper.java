package org.tasks.gtasks;

import android.app.Activity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.preferences.Preferences;

public class GtaskSyncAdapterHelper {

  private final Preferences preferences;
  private final PlayServices playServices;
  private final GoogleTaskListDao googleTaskListDao;
  private final Tracker tracker;

  @Inject
  public GtaskSyncAdapterHelper(
      Preferences preferences,
      PlayServices playServices,
      GoogleTaskListDao googleTaskListDao,
      Tracker tracker) {
    this.preferences = preferences;
    this.playServices = playServices;
    this.googleTaskListDao = googleTaskListDao;
    this.tracker = tracker;
  }

  public boolean isEnabled() {
    return hasAccounts() && playServices.isPlayServicesAvailable();
  }

  private boolean hasAccounts() {
    return !googleTaskListDao.getAccounts().isEmpty();
  }

  public Disposable checkPlayServices(Activity activity) {
    return googleTaskListDao
        .accountCount()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            count -> {
              if (count > 0
                  && !playServices.refreshAndCheck()
                  && !preferences.getBoolean(R.string.warned_play_services, false)) {
                preferences.setBoolean(R.string.warned_play_services, true);
                playServices.resolve(activity);
                tracker.reportEvent(
                    Tracking.Events.PLAY_SERVICES_WARNING, playServices.getStatus());
              }
            });
  }
}
