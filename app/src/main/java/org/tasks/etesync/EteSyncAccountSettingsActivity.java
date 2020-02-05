package org.tasks.etesync;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProviders;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import com.todoroo.astrid.helper.UUIDHelper;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracking.Events;
import org.tasks.caldav.BaseCaldavAccountSettingsActivity;
import org.tasks.data.CaldavAccount;
import org.tasks.gtasks.PlayServices;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ForApplication;

public class EteSyncAccountSettingsActivity extends BaseCaldavAccountSettingsActivity
    implements Toolbar.OnMenuItemClickListener {

  @Inject @ForApplication Context context;
  @Inject PlayServices playServices;
  @Inject EteSyncClient eteSyncClient;

  private AddEteSyncAccountViewModel addAccountViewModel;
  private UpdateEteSyncAccountViewModel updateAccountViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding.repeat.setVisibility(View.GONE);
    binding.encryptionPasswordLayout.setVisibility(View.VISIBLE);

    addAccountViewModel = ViewModelProviders.of(this).get(AddEteSyncAccountViewModel.class);
    updateAccountViewModel = ViewModelProviders.of(this).get(UpdateEteSyncAccountViewModel.class);

    if (savedInstanceState == null) {
      if (caldavAccount == null) {
        binding.url.setText(R.string.etesync_url);
      }
    }

    addAccountViewModel.observe(this, this::addAccount, this::requestFailed);
    updateAccountViewModel.observe(this, this::updateAccount, this::requestFailed);
  }

  private void addAccount(Pair<String, String> authentication) {
    CaldavAccount newAccount = new CaldavAccount();
    newAccount.setAccountType(CaldavAccount.TYPE_ETESYNC);
    newAccount.setUuid(UUIDHelper.newUUID());
    applyTo(newAccount, authentication);
    newAccount.setId(caldavDao.insert(newAccount));

    tracker.reportEvent(Events.CALDAV_ACCOUNT_ADDED);

    setResult(RESULT_OK);
    finish();
  }

  private void updateAccount(Pair<String, String> authentication) {
    applyTo(caldavAccount, authentication);
    caldavAccount.setError("");
    caldavDao.update(caldavAccount);
    setResult(RESULT_OK);
    finish();
  }

  private void applyTo(CaldavAccount account, @Nullable Pair<String, String> authentication) {
    account.setName(getNewName());
    account.setUrl(getNewURL());
    account.setUsername(getNewUsername());
    if (authentication != null) {
      account.setPassword(encryption.encrypt(authentication.first));
      account.setEncryptionKey(encryption.encrypt(authentication.second));
    }
  }

  @Override
  protected boolean needsValidation() {
    return super.needsValidation() || encryptionPasswordChanged();
  }

  protected boolean encryptionPasswordChanged() {
    return caldavAccount == null
        || !PASSWORD_MASK.equals(binding.encryptionPassword.getText().toString().trim());
  }

  @Override
  protected void addAccount(String url, String username, String password) {
    addAccountViewModel.addAccount(
        playServices, context, eteSyncClient, url, username, password, getNewEncryptionPassword());
  }

  @Override
  protected void updateAccount(String url, String username, String password) {
    updateAccountViewModel.updateAccount(
        eteSyncClient, url, username, password, getNewEncryptionPassword());
  }

  @Override
  protected void updateAccount() {
    updateAccount(null);
  }

  @Override
  protected String getHelpUrl() {
    return "http://tasks.org/etesync";
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @OnTextChanged(R.id.encryption_password)
  void onEncryptionPasswordChanged(CharSequence text) {
    binding.encryptionPasswordLayout.setError(null);
  }

  @OnFocusChange(R.id.encryption_password)
  void onEncryptionPasswordFocused(boolean hasFocus) {
    changePasswordFocus(binding.encryptionPassword, hasFocus);
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
