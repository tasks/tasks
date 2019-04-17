package org.tasks.gtasks;

import static io.reactivex.Single.fromCallable;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.LocationDao;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class PlayServices {

  private static final int REQUEST_RESOLUTION = 10000;

  private final Context context;
  private final Preferences preferences;
  private final GoogleTaskListDao googleTaskListDao;
  private final LocationDao locationDao;

  @Inject
  public PlayServices(
      @ForApplication Context context,
      Preferences preferences,
      GoogleTaskListDao googleTaskListDao,
      LocationDao locationDao) {
    this.context = context;
    this.preferences = preferences;
    this.googleTaskListDao = googleTaskListDao;
    this.locationDao = locationDao;
  }

  public Disposable check(Activity activity) {
    return Single.zip(
            googleTaskListDao.accountCount(),
            locationDao.geofenceCount(),
            fromCallable(() -> preferences.getBoolean(R.string.p_google_drive_backup, false)),
            (gtaskCount, geofenceCount, gdrive) -> gtaskCount > 0 || geofenceCount > 0 || gdrive)
        .subscribeOn(Schedulers.io())
        .map(needsCheck -> !needsCheck || refreshAndCheck())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            success -> {
              if (!success && !preferences.getBoolean(R.string.warned_play_services, false)) {
                preferences.setBoolean(R.string.warned_play_services, true);
                resolve(activity);
              }
            });
  }

  public boolean refreshAndCheck() {
    refresh();
    return isPlayServicesAvailable();
  }

  public boolean isPlayServicesAvailable() {
    return getResult() == ConnectionResult.SUCCESS;
  }

  private void refresh() {
    GoogleApiAvailability instance = GoogleApiAvailability.getInstance();
    int googlePlayServicesAvailable = instance.isGooglePlayServicesAvailable(context);
    preferences.setInt(R.string.play_services_available, googlePlayServicesAvailable);
    if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
      preferences.setBoolean(R.string.warned_play_services, false);
    }
    Timber.d(getStatus());
  }

  public void resolve(Activity activity) {
    GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
    int error = preferences.getInt(R.string.play_services_available, -1);
    if (googleApiAvailability.isUserResolvableError(error)) {
      googleApiAvailability.getErrorDialog(activity, error, REQUEST_RESOLUTION).show();
    } else {
      Toast.makeText(activity, getStatus(), Toast.LENGTH_LONG).show();
    }
  }

  public String getStatus() {
    return GoogleApiAvailability.getInstance().getErrorString(getResult());
  }

  private int getResult() {
    return preferences.getInt(R.string.play_services_available, -1);
  }

}
