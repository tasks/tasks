package org.tasks.etesync;

import static org.tasks.Strings.isNullOrEmpty;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.lifecycle.ViewModelProvider;
import at.bitfire.dav4jvm.exception.HttpException;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import com.etesync.journalmanager.Constants;
import com.etesync.journalmanager.Crypto;
import com.etesync.journalmanager.Crypto.CryptoManager;
import com.etesync.journalmanager.Exceptions.IntegrityException;
import com.etesync.journalmanager.Exceptions.VersionTooNewException;
import com.etesync.journalmanager.UserInfoManager.UserInfo;
import com.google.android.material.snackbar.Snackbar;
import java.net.ConnectException;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.CaldavAccount;
import org.tasks.databinding.ActivityEtesyncEncryptionSettingsBinding;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.security.KeyStoreEncryption;
import org.tasks.ui.DisplayableException;
import timber.log.Timber;

public class EncryptionSettingsActivity extends ThemedInjectingAppCompatActivity
    implements OnMenuItemClickListener {

  public static final String EXTRA_USER_INFO = "extra_user_info";
  public static final String EXTRA_ACCOUNT = "extra_account";
  public static final String EXTRA_DERIVED_KEY = "extra_derived_key";

  @Inject EteSyncClient client;
  @Inject KeyStoreEncryption encryption;

  private ActivityEtesyncEncryptionSettingsBinding binding;
  private UserInfo userInfo;
  private CaldavAccount caldavAccount;
  private CreateUserInfoViewModel createUserInfoViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    createUserInfoViewModel = new ViewModelProvider(this).get(CreateUserInfoViewModel.class);

    binding = ActivityEtesyncEncryptionSettingsBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    ButterKnife.bind(this);

    Intent intent = getIntent();
    caldavAccount = intent.getParcelableExtra(EXTRA_ACCOUNT);
    userInfo = (UserInfo) intent.getSerializableExtra(EXTRA_USER_INFO);

    if (userInfo == null) {
      binding.description.setVisibility(View.VISIBLE);
      binding.repeatEncryptionPasswordLayout.setVisibility(View.VISIBLE);
    }

    Toolbar toolbar = binding.toolbar.toolbar;
    toolbar.setTitle(
        caldavAccount == null ? getString(R.string.add_account) : caldavAccount.getName());
    toolbar.setNavigationIcon(getDrawable(R.drawable.ic_outline_save_24px));
    toolbar.setNavigationOnClickListener(v -> save());
    toolbar.inflateMenu(R.menu.menu_help);
    toolbar.setOnMenuItemClickListener(this);
    themeColor.apply(toolbar);

    createUserInfoViewModel.observe(this, this::returnDerivedKey, this::requestFailed);

    if (createUserInfoViewModel.inProgress()) {
      showProgressIndicator();
    }
  }

  private void showProgressIndicator() {
    binding.progressBar.progressBar.setVisibility(View.VISIBLE);
  }

  private void hideProgressIndicator() {
    binding.progressBar.progressBar.setVisibility(View.GONE);
  }

  private boolean requestInProgress() {
    return binding.progressBar.progressBar.getVisibility() == View.VISIBLE;
  }

  private void returnDerivedKey(String derivedKey) {
    hideProgressIndicator();

    Intent result = new Intent();
    result.putExtra(EXTRA_DERIVED_KEY, derivedKey);
    setResult(RESULT_OK, result);
    finish();
  }

  private void save() {
    if (requestInProgress()) {
      return;
    }

    String encryptionPassword = getNewEncryptionPassword();
    String derivedKey = caldavAccount.getEncryptionPassword(encryption);

    if (isNullOrEmpty(encryptionPassword) && isNullOrEmpty(derivedKey)) {
      binding.encryptionPasswordLayout.setError(getString(R.string.encryption_password_required));
      return;
    }

    if (userInfo == null) {
      String repeatEncryptionPassword = binding.repeatEncryptionPassword.getText().toString().trim();
      if (!encryptionPassword.equals(repeatEncryptionPassword)) {
        binding.repeatEncryptionPasswordLayout.setError(getString(R.string.passwords_do_not_match));
        return;
      }
    }

    String key =
        isNullOrEmpty(encryptionPassword)
            ? derivedKey
            : Crypto.deriveKey(caldavAccount.getUsername(), encryptionPassword);
    CryptoManager cryptoManager;
    try {
      int version = userInfo == null ? Constants.CURRENT_VERSION : userInfo.getVersion();
      cryptoManager = new CryptoManager(version, key, "userInfo");
    } catch (VersionTooNewException | IntegrityException e) {
      requestFailed(e);
      return;
    }

    if (userInfo == null) {
      showProgressIndicator();
      createUserInfoViewModel.createUserInfo(client, caldavAccount, key);
    } else {
      try {
        userInfo.verify(cryptoManager);
        returnDerivedKey(key);
      } catch (IntegrityException e) {
        binding.encryptionPasswordLayout.setError(getString(R.string.encryption_password_wrong));
      }
    }
  }

  protected void requestFailed(Throwable t) {
    hideProgressIndicator();

    if (t instanceof HttpException) {
      showSnackbar(t.getMessage());
    } else if (t instanceof DisplayableException) {
      showSnackbar(((DisplayableException) t).getResId());
    } else if (t instanceof ConnectException) {
      showSnackbar(R.string.network_error);
    } else {
      Timber.e(t);
      showSnackbar(R.string.error_adding_account, t.getMessage());
    }
  }

  private void showSnackbar(int resId, Object... formatArgs) {
    showSnackbar(getString(resId, formatArgs));
  }

  private void showSnackbar(String message) {
    newSnackbar(message).show();
  }

  private Snackbar newSnackbar(String message) {
    Snackbar snackbar =
        Snackbar.make(binding.rootLayout, message, 8000)
            .setTextColor(getColor(R.color.snackbar_text_color))
            .setActionTextColor(getColor(R.color.snackbar_action_color));
    snackbar
        .getView()
        .setBackgroundColor(getColor(R.color.snackbar_background));
    return snackbar;
  }

  @OnTextChanged(R.id.repeat_encryption_password)
  void onRpeatEncryptionPasswordChanged() {
    binding.repeatEncryptionPasswordLayout.setError(null);
  }

  @OnTextChanged(R.id.encryption_password)
  void onEncryptionPasswordChanged() {
    binding.encryptionPasswordLayout.setError(null);
  }

  private String getNewEncryptionPassword() {
    return binding.encryptionPassword.getText().toString().trim();
  }

  @Override
  public void finish() {
    if (!requestInProgress()) {
      super.finish();
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.menu_help) {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://tasks.org/etesync")));
      return true;
    } else {
      return onOptionsItemSelected(item);
    }
  }
}
