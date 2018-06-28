package org.tasks.caldav;

import static android.text.TextUtils.isEmpty;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import at.bitfire.dav4android.exception.HttpException;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import com.todoroo.astrid.helper.UUIDHelper;
import com.todoroo.astrid.service.TaskDeleter;
import java.net.ConnectException;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking.Events;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.security.Encryption;
import org.tasks.sync.SyncAdapters;
import org.tasks.ui.DisplayableException;
import org.tasks.ui.MenuColorizer;
import timber.log.Timber;

public class CaldavAccountSettingsActivity extends ThemedInjectingAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  public static final String EXTRA_CALDAV_DATA = "caldavData"; // $NON-NLS-1$
  private static final String PASSWORD_MASK = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022";
  @Inject DialogBuilder dialogBuilder;
  @Inject Preferences preferences;
  @Inject Tracker tracker;
  @Inject CaldavDao caldavDao;
  @Inject SyncAdapters syncAdapters;
  @Inject TaskDeleter taskDeleter;
  @Inject Encryption encryption;

  @BindView(R.id.root_layout)
  LinearLayout root;

  @BindView(R.id.name)
  TextInputEditText name;

  @BindView(R.id.url)
  TextInputEditText url;

  @BindView(R.id.user)
  TextInputEditText user;

  @BindView(R.id.password)
  TextInputEditText password;

  @BindView(R.id.name_layout)
  TextInputLayout nameLayout;

  @BindView(R.id.url_layout)
  TextInputLayout urlLayout;

  @BindView(R.id.user_layout)
  TextInputLayout userLayout;

  @BindView(R.id.password_layout)
  TextInputLayout passwordLayout;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  private CaldavAccount caldavAccount;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_caldav_account_settings);

    ButterKnife.bind(this);

    caldavAccount = getIntent().getParcelableExtra(EXTRA_CALDAV_DATA);

    if (savedInstanceState == null) {
      if (caldavAccount != null) {
        name.setText(caldavAccount.getName());
        url.setText(caldavAccount.getUrl());
        user.setText(caldavAccount.getUsername());
        if (!isEmpty(caldavAccount.getPassword())) {
          password.setText(PASSWORD_MASK);
        }
      }
    }

    final boolean backButtonSavesTask = preferences.backButtonSavesTask();
    toolbar.setTitle(
        caldavAccount == null ? getString(R.string.add_account) : caldavAccount.getName());
    toolbar.setNavigationIcon(
        ContextCompat.getDrawable(
            this, backButtonSavesTask ? R.drawable.ic_close_24dp : R.drawable.ic_save_24dp));
    toolbar.setNavigationOnClickListener(
        v -> {
          if (backButtonSavesTask) {
            discard();
          } else {
            save();
          }
        });
    toolbar.inflateMenu(R.menu.menu_caldav_account_settings);
    toolbar.setOnMenuItemClickListener(this);
    toolbar.showOverflowMenu();
    MenuColorizer.colorToolbar(this, toolbar);

    if (caldavAccount == null) {
      toolbar.getMenu().findItem(R.id.remove).setVisible(false);
      name.requestFocus();
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT);
    }
  }

  @OnTextChanged(R.id.name)
  void onNameChanged(CharSequence text) {
    nameLayout.setError(null);
  }

  @OnTextChanged(R.id.url)
  void onUrlChanged(CharSequence text) {
    urlLayout.setError(null);
  }

  @OnTextChanged(R.id.user)
  void onUserChanged(CharSequence text) {
    userLayout.setError(null);
  }

  @OnTextChanged(R.id.password)
  void onPasswordChanged(CharSequence text) {
    passwordLayout.setError(null);
  }

  @OnFocusChange(R.id.password)
  void onPasswordFocused(boolean hasFocus) {
    if (hasFocus) {
      if (PASSWORD_MASK.equals(password.getText().toString())) {
        password.setText("");
      }
    } else {
      if (isEmpty(password.getText()) && caldavAccount != null) {
        password.setText(PASSWORD_MASK);
      }
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  private String getNewName() {
    return name.getText().toString().trim();
  }

  private String getNewURL() {
    return url.getText().toString().trim();
  }

  private String getNewUsername() {
    return user.getText().toString().trim();
  }

  private boolean passwordChanged() {
    return caldavAccount == null || !PASSWORD_MASK.equals(password.getText().toString().trim());
  }

  private String getNewPassword() {
    String input = password.getText().toString().trim();
    return PASSWORD_MASK.equals(input) ? encryption.decrypt(caldavAccount.getPassword()) : input;
  }

  private void save() {
    String name = getNewName();
    String username = getNewUsername();
    String url = getNewURL();
    String password = getNewPassword();

    boolean failed = false;

    if (isEmpty(name)) {
      nameLayout.setError(getString(R.string.name_cannot_be_empty));
      failed = true;
    } else {
      CaldavAccount accountByName = caldavDao.getAccountByName(name);
      if (accountByName != null && !accountByName.equals(caldavAccount)) {
        nameLayout.setError(getString(R.string.duplicate_name));
        failed = true;
      }
    }

    if (isEmpty(url)) {
      urlLayout.setError(getString(R.string.url_required));
      failed = true;
    } else {
      Uri baseURL = Uri.parse(url);
      String scheme = baseURL.getScheme();
      if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
        String host = baseURL.getHost();
        if (isEmpty(host)) {
          urlLayout.setError(getString(R.string.url_host_name_required));
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
            urlLayout.setError(e.getLocalizedMessage());
            failed = true;
          }
        }
      } else {
        urlLayout.setError(getString(R.string.url_invalid_scheme));
        failed = true;
      }
    }

    if (isEmpty(username)) {
      userLayout.setError(getString(R.string.username_required));
      failed = true;
    }

    if (isEmpty(password)) {
      passwordLayout.setError(getString(R.string.password_required));
      failed = true;
    }

    if (failed) {
      return;
    }

    if (caldavAccount == null) {
      CaldavClient client = new CaldavClient(url, username, password);
      ProgressDialog dialog = dialogBuilder.newProgressDialog(R.string.contacting_server);
      dialog.show();
      client
          .getHomeSet()
          .doAfterTerminate(dialog::dismiss)
          .subscribe(this::addAccount, this::requestFailed);
    } else if (needsValidation()) {
      CaldavClient client = new CaldavClient(url, username, password);
      ProgressDialog dialog = dialogBuilder.newProgressDialog(R.string.contacting_server);
      dialog.show();
      client
          .getHomeSet()
          .doAfterTerminate(dialog::dismiss)
          .subscribe(this::updateAccount, this::requestFailed);
    } else if (hasChanges()) {
      updateAccount(caldavAccount.getUrl());
    } else {
      finish();
    }
  }

  private void addAccount(String principal) {
    Timber.d("Found principal: %s", principal);

    CaldavAccount newAccount = new CaldavAccount();
    newAccount.setName(getNewName());
    newAccount.setUrl(principal);
    newAccount.setUsername(getNewUsername());
    newAccount.setPassword(encryption.encrypt(getNewPassword()));
    newAccount.setUuid(UUIDHelper.newUUID());
    newAccount.setId(caldavDao.insert(newAccount));

    tracker.reportEvent(Events.CALDAV_ACCOUNT_ADDED);

    setResult(RESULT_OK);
    finish();
  }

  private void updateAccount(String principal) {
    caldavAccount.setName(getNewName());
    caldavAccount.setUrl(principal);
    caldavAccount.setUsername(getNewUsername());
    caldavAccount.setError("");
    if (passwordChanged()) {
      caldavAccount.setPassword(encryption.encrypt(getNewPassword()));
    }
    caldavDao.update(caldavAccount);

    setResult(RESULT_OK);
    finish();
  }

  private void requestFailed(Throwable t) {
    if (t instanceof HttpException) {
      showSnackbar(t.getMessage());
    } else if (t instanceof DisplayableException) {
      showSnackbar(((DisplayableException) t).getResId());
    } else if (t instanceof ConnectException) {
      showSnackbar(R.string.network_error);
    } else {
      Timber.e(t);
      showSnackbar(R.string.error_adding_account);
    }
  }

  private void showSnackbar(int resId) {
    showSnackbar(getString(resId));
  }

  private void showSnackbar(String message) {
    Snackbar snackbar =
        Snackbar.make(root, message, 8000)
            .setActionTextColor(ContextCompat.getColor(this, R.color.snackbar_text_color));
    snackbar
        .getView()
        .setBackgroundColor(ContextCompat.getColor(this, R.color.snackbar_background));
    snackbar.show();
  }

  private boolean hasChanges() {
    if (caldavAccount == null) {
      return !isEmpty(getNewName())
          || !isEmpty(getNewPassword())
          || !isEmpty(getNewURL())
          || !isEmpty(getNewUsername());
    }
    return needsValidation() || !getNewName().equals(caldavAccount.getName());
  }

  private boolean needsValidation() {
    return !getNewURL().equals(caldavAccount.getUrl())
        || !getNewUsername().equals(caldavAccount.getUsername())
        || passwordChanged();
  }

  @Override
  public void finish() {
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(name.getWindowToken(), 0);
    super.finish();
  }

  @Override
  public void onBackPressed() {
    if (preferences.backButtonSavesTask()) {
      save();
    } else {
      discard();
    }
  }

  private void removeAccount() {
    dialogBuilder
        .newMessageDialog(R.string.logout_warning, caldavAccount.getName())
        .setPositiveButton(
            R.string.remove,
            (dialog, which) -> {
              taskDeleter.delete(caldavAccount);
              tracker.reportEvent(Events.CALDAV_ACCOUNT_REMOVED);
              setResult(RESULT_OK);
              finish();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void discard() {
    if (!hasChanges()) {
      finish();
    } else {
      dialogBuilder
          .newMessageDialog(R.string.discard_changes)
          .setPositiveButton(R.string.discard, (dialog, which) -> finish())
          .setNegativeButton(android.R.string.cancel, null)
          .show();
    }
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.help:
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://tasks.org/caldav")));
        break;
      case R.id.remove:
        removeAccount();
        break;
    }
    return onOptionsItemSelected(item);
  }
}
