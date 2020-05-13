package org.tasks.caldav;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import com.todoroo.astrid.helper.UUIDHelper;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.CaldavAccount;
import org.tasks.injection.ActivityComponent;
import timber.log.Timber;

public class CaldavAccountSettingsActivity extends BaseCaldavAccountSettingsActivity
    implements Toolbar.OnMenuItemClickListener {

  @Inject CaldavClient client;

  private AddCaldavAccountViewModel addCaldavAccountViewModel;
  private UpdateCaldavAccountViewModel updateCaldavAccountViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ViewModelProvider provider = new ViewModelProvider(this);
    addCaldavAccountViewModel = provider.get(AddCaldavAccountViewModel.class);
    updateCaldavAccountViewModel = provider.get(UpdateCaldavAccountViewModel.class);

    addCaldavAccountViewModel.observe(this, this::addAccount, this::requestFailed);
    updateCaldavAccountViewModel.observe(this, this::updateAccount, this::requestFailed);
  }

  @Override
  protected int getDescription() {
    return R.string.caldav_account_description;
  }

  private void addAccount(String principal) {
    hideProgressIndicator();

    Timber.d("Found principal: %s", principal);

    CaldavAccount newAccount = new CaldavAccount();
    newAccount.setName(getNewName());
    newAccount.setUrl(principal);
    newAccount.setUsername(getNewUsername());
    newAccount.setPassword(encryption.encrypt(getNewPassword()));
    newAccount.setUuid(UUIDHelper.newUUID());
    newAccount.setId(caldavDao.insert(newAccount));

    setResult(RESULT_OK);
    finish();
  }

  private void updateAccount(String principal) {
    hideProgressIndicator();

    caldavAccount.setName(getNewName());
    caldavAccount.setUrl(principal);
    caldavAccount.setUsername(getNewUsername());
    caldavAccount.setError("");
    if (passwordChanged()) {
      caldavAccount.setPassword(encryption.encrypt(getNewPassword()));
    }
    caldavAccount.setSuppressRepeatingTasks(binding.repeat.isChecked());
    caldavDao.update(caldavAccount);

    setResult(RESULT_OK);
    finish();
  }

  @Override
  protected void addAccount(String url, String username, String password) {
    addCaldavAccountViewModel.addAccount(client, url, username, password);
  }

  @Override
  protected void updateAccount(String url, String username, String password) {
    updateCaldavAccountViewModel.updateCaldavAccount(client, url, username, password);
  }

  @Override
  protected void updateAccount() {
    updateAccount(caldavAccount.getUrl());
  }

  @Override
  protected String getNewPassword() {
    String input = binding.password.getText().toString().trim();
    return PASSWORD_MASK.equals(input) ? encryption.decrypt(caldavAccount.getPassword()) : input;
  }

  @Override
  protected String getHelpUrl() {
    return "https://tasks.org/caldav";
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
