/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.gtasks.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.todoroo.andlib.utility.DialogUtilities;
import io.reactivex.disposables.CompositeDisposable;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.play.AuthResultHandler;

/**
 * This activity allows users to sign in or log in to Google Tasks through the Android account
 * manager
 *
 * @author Sam Bosley
 */
public class GtasksLoginActivity extends InjectingAppCompatActivity {

  private static final int RC_CHOOSE_ACCOUNT = 10988;
  @Inject DialogBuilder dialogBuilder;
  @Inject GoogleAccountManager googleAccountManager;
  @Inject GoogleTaskListDao googleTaskListDao;
  private CompositeDisposable disposables;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent chooseAccountIntent =
        android.accounts.AccountManager.newChooseAccountIntent(
            null, null, new String[] {"com.google"}, false, null, null, null, null);
    startActivityForResult(chooseAccountIntent, RC_CHOOSE_ACCOUNT);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  private void getAuthToken(String account) {
    final ProgressDialog pd = dialogBuilder.newProgressDialog(R.string.gtasks_GLA_authenticating);
    pd.show();
    getAuthToken(account, pd);
  }

  @Override
  protected void onPause() {
    super.onPause();

    if (disposables != null) {
      disposables.dispose();
    }
  }

  private void getAuthToken(String a, final ProgressDialog pd) {
    disposables =
        new CompositeDisposable(
            googleAccountManager.getTasksAuthToken(
                this,
                a,
                new AuthResultHandler() {
                  @Override
                  public void authenticationSuccessful(String accountName) {
                    GoogleTaskAccount account = googleTaskListDao.getAccount(accountName);
                    if (account == null) {
                      account = new GoogleTaskAccount();
                      account.setAccount(accountName);
                      googleTaskListDao.insert(account);
                    } else {
                      account.setError("");
                      googleTaskListDao.update(account);
                    }
                    setResult(RESULT_OK);
                    finish();
                    DialogUtilities.dismissDialog(GtasksLoginActivity.this, pd);
                  }

                  @Override
                  public void authenticationFailed(final String message) {
                    runOnUiThread(
                        () ->
                            Toast.makeText(GtasksLoginActivity.this, message, Toast.LENGTH_LONG)
                                .show());
                    DialogUtilities.dismissDialog(GtasksLoginActivity.this, pd);
                  }
                }));
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == RC_CHOOSE_ACCOUNT) {
      if (resultCode == RESULT_OK) {
        String account = data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME);
        getAuthToken(account);
      } else {
        finish();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
