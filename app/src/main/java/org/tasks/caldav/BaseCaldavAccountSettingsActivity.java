package org.tasks.caldav;

import static android.text.TextUtils.isEmpty;
import static com.todoroo.astrid.data.Task.NO_ID;
import static org.tasks.billing.PurchaseDialog.newPurchaseDialog;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import at.bitfire.dav4jvm.exception.HttpException;
import butterknife.ButterKnife;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Strings;
import com.todoroo.astrid.service.TaskDeleter;
import java.net.ConnectException;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.billing.Inventory;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.databinding.ActivityCaldavAccountSettingsBinding;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.security.Encryption;
import org.tasks.ui.DisplayableException;
import org.tasks.ui.MenuColorizer;
import timber.log.Timber;

public abstract class BaseCaldavAccountSettingsActivity extends ThemedInjectingAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  public static final String EXTRA_CALDAV_DATA = "caldavData"; // $NON-NLS-1$
  protected static final String PASSWORD_MASK = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022";
  @Inject protected CaldavDao caldavDao;
  @Inject protected Encryption encryption;
  @Inject DialogBuilder dialogBuilder;
  @Inject TaskDeleter taskDeleter;
  @Inject Inventory inventory;

  protected CaldavAccount caldavAccount;

  protected ActivityCaldavAccountSettingsBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivityCaldavAccountSettingsBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    ButterKnife.bind(this);

    caldavAccount =
        savedInstanceState == null
            ? getIntent().getParcelableExtra(EXTRA_CALDAV_DATA)
            : savedInstanceState.getParcelable(EXTRA_CALDAV_DATA);

    if (caldavAccount == null || caldavAccount.getId() == NO_ID) {
      binding.nameLayout.setVisibility(View.GONE);
      binding.description.setVisibility(View.VISIBLE);
      binding.description.setText(getDescription());
      Linkify.addLinks(binding.description, Linkify.WEB_URLS);
    } else {
      binding.nameLayout.setVisibility(View.VISIBLE);
      binding.description.setVisibility(View.GONE);
    }

    if (savedInstanceState == null) {
      if (caldavAccount != null) {
        binding.name.setText(caldavAccount.getName());
        binding.url.setText(caldavAccount.getUrl());
        binding.user.setText(caldavAccount.getUsername());
        if (!isEmpty(caldavAccount.getPassword())) {
          binding.password.setText(PASSWORD_MASK);
        }
        binding.repeat.setChecked(caldavAccount.isSuppressRepeatingTasks());
      }
    }

    Toolbar toolbar = binding.toolbar.toolbar;

    toolbar.setTitle(
        caldavAccount == null ? getString(R.string.add_account) : caldavAccount.getName());
    toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_outline_save_24px));
    toolbar.setNavigationOnClickListener(v -> save());
    toolbar.inflateMenu(R.menu.menu_caldav_account_settings);
    toolbar.setOnMenuItemClickListener(this);
    toolbar.showOverflowMenu();
    MenuColorizer.colorToolbar(this, toolbar);

    if (caldavAccount == null) {
      toolbar.getMenu().findItem(R.id.remove).setVisible(false);
      binding.name.requestFocus();
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(binding.name, InputMethodManager.SHOW_IMPLICIT);
    }

    if (!inventory.hasPro()) {
      newSnackbar(getString(R.string.this_feature_requires_a_subscription))
          .setDuration(BaseTransientBottomBar.LENGTH_INDEFINITE)
          .setAction(
              R.string.button_subscribe,
              v -> newPurchaseDialog().show(getSupportFragmentManager(), null))
          .show();
    }
  }

  protected abstract @StringRes int getDescription();

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelable(EXTRA_CALDAV_DATA, caldavAccount);
  }

  private void showProgressIndicator() {
    binding.progressBar.progressBar.setVisibility(View.VISIBLE);
  }

  protected void hideProgressIndicator() {
    binding.progressBar.progressBar.setVisibility(View.GONE);
  }

  private boolean requestInProgress() {
    return binding.progressBar.progressBar.getVisibility() == View.VISIBLE;
  }

  @OnTextChanged(R.id.name)
  void onNameChanged() {
    binding.nameLayout.setError(null);
  }

  @OnTextChanged(R.id.url)
  void onUrlChanged() {
    binding.urlLayout.setError(null);
  }

  @OnTextChanged(R.id.user)
  void onUserChanged() {
    binding.userLayout.setError(null);
  }

  @OnTextChanged(R.id.password)
  void onPasswordChanged() {
    binding.passwordLayout.setError(null);
  }

  @OnFocusChange(R.id.password)
  void onPasswordFocused(boolean hasFocus) {
    if (hasFocus) {
      if (PASSWORD_MASK.equals(binding.password.getText().toString())) {
        binding.password.setText("");
      }
    } else {
      if (isEmpty(binding.password.getText()) && caldavAccount != null) {
        binding.password.setText(PASSWORD_MASK);
      }
    }
  }

  protected String getNewName() {
    String name = binding.name.getText().toString().trim();
    return Strings.isNullOrEmpty(name) ? getNewUsername() : name;
  }

  protected String getNewURL() {
    return binding.url.getText().toString().trim();
  }

  protected String getNewUsername() {
    return binding.user.getText().toString().trim();
  }

  protected boolean passwordChanged() {
    return caldavAccount == null || !PASSWORD_MASK.equals(binding.password.getText().toString().trim());
  }

  protected abstract String getNewPassword();

  private void save() {
    if (requestInProgress()) {
      return;
    }

    String username = getNewUsername();
    String url = getNewURL();
    String password = getNewPassword();

    boolean failed = false;

    if (isEmpty(url)) {
      binding.urlLayout.setError(getString(R.string.url_required));
      failed = true;
    } else {
      Uri baseURL = Uri.parse(url);
      String scheme = baseURL.getScheme();
      if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
        String host = baseURL.getHost();
        if (isEmpty(host)) {
          binding.urlLayout.setError(getString(R.string.url_host_name_required));
          failed = true;
        } else {
          try {
            host = IDN.toASCII(host);
          } catch (Exception e) {
            Timber.e(e);
          }
          String path = baseURL.getEncodedPath();
          int port = baseURL.getPort();
          try {
            new URI(scheme, null, host, port, path, null, null);
          } catch (URISyntaxException e) {
            binding.urlLayout.setError(e.getLocalizedMessage());
            failed = true;
          }
        }
      } else {
        binding.urlLayout.setError(getString(R.string.url_invalid_scheme));
        failed = true;
      }
    }

    if (isEmpty(username)) {
      binding.userLayout.setError(getString(R.string.username_required));
      failed = true;
    }

    if (isEmpty(password)) {
      binding.passwordLayout.setError(getString(R.string.password_required));
      failed = true;
    }

    if (failed) {
      return;
    }

    if (caldavAccount == null) {
      showProgressIndicator();
      addAccount(url, username, password);
    } else if (needsValidation()) {
      showProgressIndicator();
      updateAccount(url, username, password);
    } else if (hasChanges()) {
      updateAccount();
    } else {
      finish();
    }
  }

  protected abstract void addAccount(String url, String username, String password);

  protected abstract void updateAccount(String url, String username, String password);

  protected abstract void updateAccount();

  protected abstract String getHelpUrl();

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
            .setTextColor(ContextCompat.getColor(this, R.color.snackbar_text_color))
            .setActionTextColor(ContextCompat.getColor(this, R.color.snackbar_action_color));
    snackbar
        .getView()
        .setBackgroundColor(ContextCompat.getColor(this, R.color.snackbar_background));
    return snackbar;
  }

  private boolean hasChanges() {
    if (caldavAccount == null) {
      return !isEmpty(binding.name.getText().toString().trim())
          || !isEmpty(getNewPassword())
          || !isEmpty(binding.url.getText().toString().trim())
          || !isEmpty(getNewUsername())
          || binding.repeat.isChecked();
    }
    return needsValidation()
        || !getNewName().equals(caldavAccount.getName())
        || binding.repeat.isChecked() != caldavAccount.isSuppressRepeatingTasks();
  }

  protected boolean needsValidation() {
    return !getNewURL().equals(caldavAccount.getUrl())
        || !getNewUsername().equals(caldavAccount.getUsername())
        || passwordChanged();
  }

  @Override
  public void finish() {
    if (!requestInProgress()) {
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(binding.name.getWindowToken(), 0);
      super.finish();
    }
  }

  @Override
  public void onBackPressed() {
    discard();
  }

  private void removeAccountPrompt() {
    if (requestInProgress()) {
      return;
    }

    dialogBuilder
        .newDialog()
        .setMessage(R.string.logout_warning, caldavAccount.getName())
        .setPositiveButton(R.string.remove, (dialog, which) -> removeAccount())
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  protected void removeAccount() {
    taskDeleter.delete(caldavAccount);
    setResult(RESULT_OK);
    finish();
  }

  private void discard() {
    if (requestInProgress()) {
      return;
    }

    if (!hasChanges()) {
      finish();
    } else {
      dialogBuilder
          .newDialog(R.string.discard_changes)
          .setPositiveButton(R.string.discard, (dialog, which) -> finish())
          .setNegativeButton(android.R.string.cancel, null)
          .show();
    }
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.help:
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getHelpUrl())));
        break;
      case R.id.remove:
        removeAccountPrompt();
        break;
    }
    return onOptionsItemSelected(item);
  }
}
