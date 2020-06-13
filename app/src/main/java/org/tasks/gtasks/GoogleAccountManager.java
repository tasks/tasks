package org.tasks.gtasks;

import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.transform;
import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;
import static java.util.Arrays.asList;
import static org.tasks.Strings.isNullOrEmpty;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.tasks.TasksScopes;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ApplicationContext;
import org.tasks.play.AuthResultHandler;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class GoogleAccountManager {

  private final PermissionChecker permissionChecker;
  private final android.accounts.AccountManager accountManager;
  private final Preferences preferences;

  @Inject
  public GoogleAccountManager(
      @ApplicationContext Context context, PermissionChecker permissionChecker, Preferences preferences) {
    this.permissionChecker = permissionChecker;
    accountManager = android.accounts.AccountManager.get(context);
    this.preferences = preferences;
  }

  public List<String> getAccounts() {
    return transform(getAccountList(), account -> account.name);
  }

  private List<Account> getAccountList() {
    return permissionChecker.canAccessAccounts()
        ? asList(accountManager.getAccountsByType("com.google"))
        : Collections.emptyList();
  }

  public Account getAccount(String name) {
    if (isNullOrEmpty(name)) {
      return null;
    }

    return tryFind(getAccountList(), account -> name.equalsIgnoreCase(account.name)).orNull();
  }

  public boolean canAccessAccount(String name) {
    return getAccount(name) != null;
  }

  public String getAccessToken(String name, String scope) {
    assertNotMainThread();

    Account account = getAccount(name);
    if (account == null) {
      Timber.e("Cannot find account %s", name);
      return null;
    }

    boolean alreadyNotified = preferences.alreadyNotified(name, scope);

    try {
      String token =
          accountManager.blockingGetAuthToken(account, "oauth2:" + scope, !alreadyNotified);
      preferences.setAlreadyNotified(name, scope, isNullOrEmpty(token));
      return token;
    } catch (AuthenticatorException | IOException | OperationCanceledException e) {
      Timber.e(e);
      return null;
    }
  }

  public void getTasksAuthToken(
      final Activity activity, final String accountName, final AuthResultHandler handler) {
    getToken(TasksScopes.TASKS, activity, accountName, handler);
  }

  public void getDriveAuthToken(
      final Activity activity, final String accountName, final AuthResultHandler handler) {
    getToken(DriveScopes.DRIVE_FILE, activity, accountName, handler);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @SuppressLint("CheckResult")
  private void getToken(
      String scope, Activity activity, String accountName, AuthResultHandler handler) {
    final Account account = getAccount(accountName);
    Single.fromCallable(
            () -> {
              if (account == null) {
                throw new RuntimeException(
                    activity.getString(R.string.gtasks_error_accountNotFound, accountName));
              }
              assertNotMainThread();

              return accountManager
                  .getAuthToken(account, "oauth2:" + scope, new Bundle(), activity, null, null)
                  .getResult();
            })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            bundle -> {
              preferences.setAlreadyNotified(accountName, scope, false);
              Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
              if (intent != null) {
                activity.startActivity(intent);
              } else {
                handler.authenticationSuccessful(accountName);
              }
            },
            e -> {
              Timber.e(e);
              handler.authenticationFailed(e.getMessage());
            });
  }

  public void invalidateToken(String token) {
    accountManager.invalidateAuthToken("com.google", token);
  }
}
