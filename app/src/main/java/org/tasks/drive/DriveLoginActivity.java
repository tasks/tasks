/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package org.tasks.drive;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import com.todoroo.andlib.utility.DialogUtilities;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.gtasks.GoogleAccountManager;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.play.AuthResultHandler;
import org.tasks.preferences.Preferences;

/**
 * This activity allows users to sign in or log in to Google Tasks through the Android account
 * manager
 *
 * @author Sam Bosley
 */
@AndroidEntryPoint
public class DriveLoginActivity extends InjectingAppCompatActivity {

  public static final String EXTRA_ERROR = "extra_error";
  private static final int RC_CHOOSE_ACCOUNT = 10988;
  @Inject DialogBuilder dialogBuilder;
  @Inject GoogleAccountManager googleAccountManager;
  @Inject Preferences preferences;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent chooseAccountIntent =
        android.accounts.AccountManager.newChooseAccountIntent(
            null, null, new String[] {"com.google"}, null, null, null, null);
    startActivityForResult(chooseAccountIntent, RC_CHOOSE_ACCOUNT);
  }

  private void getAuthToken(String account) {
    final ProgressDialog pd = dialogBuilder.newProgressDialog(R.string.gtasks_GLA_authenticating);
    pd.show();
    getAuthToken(account, pd);
  }

  private void getAuthToken(String a, final ProgressDialog pd) {
    googleAccountManager.getDriveAuthToken(
        this,
        a,
        new AuthResultHandler() {
          @Override
          public void authenticationSuccessful(String accountName) {
            preferences.setString(R.string.p_google_drive_backup_account, accountName);
            preferences.setBoolean(R.string.p_google_drive_backup, true);
            setResult(RESULT_OK);
            DialogUtilities.dismissDialog(DriveLoginActivity.this, pd);
            finish();
          }

          @Override
          public void authenticationFailed(final String message) {
            preferences.setBoolean(R.string.p_google_drive_backup, false);
            setResult(RESULT_CANCELED, new Intent().putExtra(EXTRA_ERROR, message));
            DialogUtilities.dismissDialog(DriveLoginActivity.this, pd);
            finish();
          }
        });
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
