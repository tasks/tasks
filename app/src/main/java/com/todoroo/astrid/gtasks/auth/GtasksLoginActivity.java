/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.todoroo.andlib.utility.DialogUtilities;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.gtasks.PlayServices;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;

/**
 * This activity allows users to sign in or log in to Google Tasks through the Android account
 * manager
 *
 * @author Sam Bosley
 */
public class GtasksLoginActivity extends InjectingAppCompatActivity {

  public static final int RC_REQUEST_OAUTH = 10987;
  private static final int RC_CHOOSE_ACCOUNT = 10988;
  @Inject DialogBuilder dialogBuilder;
  @Inject GoogleAccountManager accountManager;
  @Inject PlayServices playServices;
  @Inject GoogleTaskListDao googleTaskListDao;
  private String accountName;

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
    accountName = account;
    getAuthToken(account, pd);
  }

  private void getAuthToken(String a, final ProgressDialog pd) {
    playServices.getAuthToken(
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
                () -> Toast.makeText(GtasksLoginActivity.this, message, Toast.LENGTH_LONG).show());
            DialogUtilities.dismissDialog(GtasksLoginActivity.this, pd);
          }
        });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == RC_CHOOSE_ACCOUNT && resultCode == RESULT_OK) {
      String account = data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME);
      getAuthToken(account);
    } else if (requestCode == RC_REQUEST_OAUTH && resultCode == RESULT_OK) {
      final ProgressDialog pd = dialogBuilder.newProgressDialog(R.string.gtasks_GLA_authenticating);
      pd.show();
      getAuthToken(accountName, pd);
    } else {
      // User didn't give permission--cancel
      finish();
    }
  }

  public interface AuthResultHandler {

    void authenticationSuccessful(String accountName);

    void authenticationFailed(String message);
  }
}
