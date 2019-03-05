package org.tasks.gtasks;

import static io.reactivex.Single.fromCallable;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.tasks.TasksScopes;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.LocationDao;
import org.tasks.injection.ForApplication;
import org.tasks.play.AuthResultHandler;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class PlayServices {

  private static final int REQUEST_RESOLUTION = 10000;

  private final Context context;
  private final Preferences preferences;
  private final GoogleAccountManager accountManager;
  private final GoogleTaskListDao googleTaskListDao;
  private final LocationDao locationDao;

  @Inject
  public PlayServices(
      @ForApplication Context context,
      Preferences preferences,
      GoogleAccountManager googleAccountManager,
      GoogleTaskListDao googleTaskListDao,
      LocationDao locationDao) {
    this.context = context;
    this.preferences = preferences;
    this.accountManager = googleAccountManager;
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

  public Disposable checkMaps(Activity activity) {
    if (preferences.useGooglePlaces() || preferences.useGoogleMaps()) {
      return Single.fromCallable(this::refreshAndCheck)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(success -> {
            if (!success) {
              resolve(activity);
            }
          });
    } else {
      return EmptyDisposable.INSTANCE;
    }
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

  public void getTasksAuthToken(
      final Activity activity, final String accountName, final AuthResultHandler handler) {
    getToken(TasksScopes.TASKS, activity, accountName, handler);
  }

  public void getDriveAuthToken(
      final Activity activity, final String accountName, final AuthResultHandler handler) {
    getToken(DriveScopes.DRIVE_FILE, activity, accountName, handler);
  }

  private void getToken(
      String scope, Activity activity, String accountName, AuthResultHandler handler) {
    final Account account = accountManager.getAccount(accountName);
    if (account == null) {
      handler.authenticationFailed(
          activity.getString(R.string.gtasks_error_accountNotFound, accountName));
    } else {
      new Thread(
              () -> {
                try {
                  GoogleAuthUtil.getToken(activity, account, "oauth2:" + scope, null);
                  handler.authenticationSuccessful(accountName);
                } catch (UserRecoverableAuthException e) {
                  Timber.e(e);
                  activity.startActivityForResult(
                      e.getIntent(), GtasksLoginActivity.RC_REQUEST_OAUTH);
                } catch (GoogleAuthException | IOException e) {
                  Timber.e(e);
                  handler.authenticationFailed(activity.getString(R.string.gtasks_GLA_errorIOAuth));
                }
              })
          .start();
    }
  }
}
