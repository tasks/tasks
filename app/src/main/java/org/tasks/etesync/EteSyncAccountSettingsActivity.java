package org.tasks.etesync;

import static com.todoroo.astrid.data.Task.NO_ID;
import static org.tasks.Strings.isNullOrEmpty;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;
import butterknife.OnCheckedChanged;
import com.etesync.journalmanager.Crypto.CryptoManager;
import com.etesync.journalmanager.Exceptions.IntegrityException;
import com.etesync.journalmanager.Exceptions.VersionTooNewException;
import com.etesync.journalmanager.UserInfoManager.UserInfo;
import com.todoroo.astrid.helper.UUIDHelper;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.caldav.BaseCaldavAccountSettingsActivity;
import org.tasks.data.CaldavAccount;
import org.tasks.injection.ActivityComponent;
import timber.log.Timber;

public class EteSyncAccountSettingsActivity extends BaseCaldavAccountSettingsActivity
    implements Toolbar.OnMenuItemClickListener {

  private static final int REQUEST_ENCRYPTION_PASSWORD = 10101;

  @Inject EteSyncClient eteSyncClient;

  private AddEteSyncAccountViewModel addAccountViewModel;
  private UpdateEteSyncAccountViewModel updateAccountViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding.repeat.setVisibility(View.GONE);
    binding.showAdvanced.setVisibility(View.VISIBLE);
    updateUrlVisibility();

    ViewModelProvider provider = new ViewModelProvider(this);
    addAccountViewModel = provider.get(AddEteSyncAccountViewModel.class);
    updateAccountViewModel = provider.get(UpdateEteSyncAccountViewModel.class);
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (!isFinishing()) {
      addAccountViewModel.observe(this, this::addAccount, this::requestFailed);
      updateAccountViewModel.observe(this, this::updateAccount, this::requestFailed);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    addAccountViewModel.removeObserver(this);
    updateAccountViewModel.removeObserver(this);
  }

  @Override
  protected int getDescription() {
    return R.string.etesync_account_description;
  }

  private void addAccount(Pair<UserInfo, String> userInfoAndToken) {
    caldavAccount = new CaldavAccount();
    caldavAccount.setAccountType(CaldavAccount.TYPE_ETESYNC);
    caldavAccount.setUuid(UUIDHelper.newUUID());
    applyTo(caldavAccount, userInfoAndToken);
  }

  private void updateAccount(Pair<UserInfo, String> userInfoAndToken) {
    caldavAccount.setError("");
    applyTo(caldavAccount, userInfoAndToken);
  }

  private void applyTo(CaldavAccount account, Pair<UserInfo, String> userInfoAndToken) {
    hideProgressIndicator();

    account.setName(getNewName());
    account.setUrl(getNewURL());
    account.setUsername(getNewUsername());
    String token = userInfoAndToken.second;
    if (!token.equals(account.getPassword(encryption))) {
      account.setPassword(encryption.encrypt(token));
    }

    UserInfo userInfo = userInfoAndToken.first;
    if (testUserInfo(userInfo)) {
      saveAccountAndFinish();
    } else {
      Intent intent = new Intent(this, EncryptionSettingsActivity.class);
      intent.putExtra(EncryptionSettingsActivity.EXTRA_USER_INFO, userInfo);
      intent.putExtra(EncryptionSettingsActivity.EXTRA_ACCOUNT, account);
      startActivityForResult(intent, REQUEST_ENCRYPTION_PASSWORD);
    }
  }

  private boolean testUserInfo(UserInfo userInfo) {
    String encryptionKey = caldavAccount.getEncryptionPassword(encryption);
    if (userInfo != null && !isNullOrEmpty(encryptionKey)) {
      try {
        CryptoManager cryptoManager =
            new CryptoManager(userInfo.getVersion(), encryptionKey, "userInfo");
        userInfo.verify(cryptoManager);
        return true;
      } catch (IntegrityException | VersionTooNewException e) {
        Timber.e(e);
      }
    }
    return false;
  }

  @OnCheckedChanged(R.id.show_advanced)
  void toggleUrl() {
    updateUrlVisibility();
  }

  private void updateUrlVisibility() {
    binding.urlLayout.setVisibility(binding.showAdvanced.isChecked() ? View.VISIBLE : View.GONE);
  }

  @Override
  protected boolean needsValidation() {
    return super.needsValidation() || isNullOrEmpty(caldavAccount.getEncryptionKey());
  }

  @Override
  protected void addAccount(String url, String username, String password) {
    addAccountViewModel.addAccount(eteSyncClient, url, username, password);
  }

  @Override
  protected void updateAccount(String url, String username, String password) {
    updateAccountViewModel.updateAccount(
        eteSyncClient,
        url,
        username,
        PASSWORD_MASK.equals(password) ? null : password,
        caldavAccount.getPassword(encryption));
  }

  @Override
  protected void updateAccount() {
    caldavAccount.setName(getNewName());
    saveAccountAndFinish();
  }

  @Override
  protected String getNewURL() {
    String url = super.getNewURL();
    return isNullOrEmpty(url) ? getString(R.string.etesync_url) : url;
  }

  @Override
  protected String getNewPassword() {
    return binding.password.getText().toString().trim();
  }

  @Override
  protected String getHelpUrl() {
    return "https://tasks.org/etesync";
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == REQUEST_ENCRYPTION_PASSWORD) {
      if (resultCode == RESULT_OK) {
        String key = data.getStringExtra(EncryptionSettingsActivity.EXTRA_DERIVED_KEY);
        caldavAccount.setEncryptionKey(encryption.encrypt(key));
        saveAccountAndFinish();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void saveAccountAndFinish() {
    if (caldavAccount.getId() == NO_ID) {
      caldavDao.insert(caldavAccount);
    } else {
      caldavDao.update(caldavAccount);
    }
    setResult(RESULT_OK);
    finish();
  }

  @Override
  protected void removeAccount() {
    if (caldavAccount != null) {
      Completable.fromAction(() -> eteSyncClient.forAccount(caldavAccount).invalidateToken())
          .subscribeOn(Schedulers.io())
          .subscribe();
    }
    super.removeAccount();
  }
}
