package org.tasks.gtasks;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.tasks.TasksScopes;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import java.io.IOException;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class PlayServices {

  private static final int REQUEST_RESOLUTION = 10000;

  private final Context context;
  private final Preferences preferences;
  private final GoogleAccountManager accountManager;

  @Inject
  public PlayServices(
      @ForApplication Context context,
      Preferences preferences,
      GoogleAccountManager googleAccountManager) {
    this.context = context;
    this.preferences = preferences;
    this.accountManager = googleAccountManager;
  }

  public boolean refreshAndCheck() {
    refresh();
    return isPlayServicesAvailable();
  }

  public boolean isPlayServicesAvailable() {
    return getResult() == ConnectionResult.SUCCESS;
  }

  public void refresh() {
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
      Toast.makeText(
              activity, R.string.common_google_play_services_notification_ticker, Toast.LENGTH_LONG)
          .show();
    }
  }

  public String getStatus() {
    return GoogleApiAvailability.getInstance().getErrorString(getResult());
  }

  private int getResult() {
    return preferences.getInt(R.string.play_services_available, -1);
  }

  public boolean clearToken(GoogleAccountCredential credential) {
    try {
      String token = credential.getToken();
      Timber.d("Invalidating %s", token);
      GoogleAuthUtil.clearToken(context, token);
      GoogleAuthUtil.getToken(
          context, credential.getSelectedAccount(), "oauth2:" + TasksScopes.TASKS, null);
      return true;
    } catch (GoogleAuthException e) {
      Timber.e(e);
      return false;
    } catch (IOException e) {
      Timber.e(e);
      return true;
    }
  }

  public void getAuthToken(
      final Activity activity,
      final String accountName,
      final GtasksLoginActivity.AuthResultHandler handler) {
    final Account account = accountManager.getAccount(accountName);
    if (account == null) {
      handler.authenticationFailed(
          activity.getString(R.string.gtasks_error_accountNotFound, accountName));
    } else {
      new Thread(
              () -> {
                try {
                  GoogleAuthUtil.getToken(activity, account, "oauth2:" + TasksScopes.TASKS, null);
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
