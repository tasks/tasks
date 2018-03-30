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
import android.text.InputType;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import at.bitfire.dav4android.exception.HttpException;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.CaldavFilter;
import com.todoroo.astrid.helper.UUIDHelper;
import java.net.ConnectException;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.activities.ColorPickerActivity;
import org.tasks.analytics.Tracker;
import org.tasks.analytics.Tracking;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavDao;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.sync.SyncAdapters;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.DisplayableException;
import timber.log.Timber;

public class CaldavSettingsActivity extends ThemedInjectingAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  public static final String EXTRA_CALDAV_DATA = "caldavData"; // $NON-NLS-1$
  public static final String ACTION_RELOAD = "accountRenamed";
  public static final String ACTION_DELETED = "accountDeleted";
  private static final String EXTRA_CALDAV_UUID = "uuid"; // $NON-NLS-1$
  private static final String EXTRA_SELECTED_THEME = "extra_selected_theme";
  private static final String PASSWORD_MASK = "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022";
  private static final int REQUEST_COLOR_PICKER = 10109;
  @Inject DialogBuilder dialogBuilder;
  @Inject Preferences preferences;
  @Inject ThemeCache themeCache;
  @Inject ThemeColor themeColor;
  @Inject Tracker tracker;
  @Inject CaldavDao caldavDao;
  @Inject SyncAdapters syncAdapters;

  @BindView(R.id.root_layout)
  LinearLayout root;

  @BindView(R.id.url)
  TextInputEditText url;

  @BindView(R.id.user)
  TextInputEditText user;

  @BindView(R.id.password)
  TextInputEditText password;

  @BindView(R.id.url_layout)
  TextInputLayout urlLayout;

  @BindView(R.id.user_layout)
  TextInputLayout userLayout;

  @BindView(R.id.password_layout)
  TextInputLayout passwordLayout;

  @BindView(R.id.color)
  TextInputEditText color;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  private CaldavAccount caldavAccount;
  private int selectedTheme;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_caldav_settings);

    ButterKnife.bind(this);

    caldavAccount = getIntent().getParcelableExtra(EXTRA_CALDAV_DATA);

    if (!isEmpty(caldavAccount.getPassword())) {
      password.setText(PASSWORD_MASK);
    }

    if (savedInstanceState == null) {
      if (caldavAccount == null) {
        selectedTheme = -1;
      } else {
        selectedTheme = caldavAccount.getColor();
        url.setText(caldavAccount.getUrl());
        user.setText(caldavAccount.getUsername());
      }
    } else {
      selectedTheme = savedInstanceState.getInt(EXTRA_SELECTED_THEME);
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
    toolbar.inflateMenu(R.menu.menu_tag_settings);
    toolbar.setOnMenuItemClickListener(this);
    toolbar.showOverflowMenu();

    color.setInputType(InputType.TYPE_NULL);

    if (caldavAccount == null) {
      toolbar.getMenu().findItem(R.id.delete).setVisible(false);
      url.requestFocus();
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(url, InputMethodManager.SHOW_IMPLICIT);
    }

    updateTheme();
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
      if (isEmpty(password.getText()) && !isEmpty(caldavAccount.getPassword())) {
        password.setText(PASSWORD_MASK);
      }
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt(EXTRA_SELECTED_THEME, selectedTheme);
  }

  @OnFocusChange(R.id.color)
  void onFocusChange(boolean focused) {
    if (focused) {
      color.clearFocus();
      showThemePicker();
    }
  }

  @OnClick(R.id.color)
  protected void showThemePicker() {
    Intent intent = new Intent(CaldavSettingsActivity.this, ColorPickerActivity.class);
    intent.putExtra(ColorPickerActivity.EXTRA_PALETTE, ColorPickerActivity.ColorPalette.COLORS);
    intent.putExtra(ColorPickerActivity.EXTRA_SHOW_NONE, true);
    startActivityForResult(intent, REQUEST_COLOR_PICKER);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  private String getNewURL() {
    return url.getText().toString().trim();
  }

  private String getNewUsername() {
    return user.getText().toString().trim();
  }

  private String getNewPassword() {
    String input = password.getText().toString().trim();
    return PASSWORD_MASK.equals(input) ? caldavAccount.getPassword() : input;
  }

  private void save() {
    String username = getNewUsername();
    String url = getNewURL();
    String password = getNewPassword();

    boolean failed = false;

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
            Timber.e(e.getMessage(), e);
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
          .getDisplayName()
          .doAfterTerminate(dialog::dismiss)
          .subscribe(this::addAccount, this::getDisplayNameFailed);
    } else if (needsValidation()) {
      CaldavClient client = new CaldavClient(url, username, password);
      ProgressDialog dialog = dialogBuilder.newProgressDialog(R.string.contacting_server);
      dialog.show();
      client
          .getDisplayName()
          .doAfterTerminate(dialog::dismiss)
          .subscribe(this::updateAccount, this::getDisplayNameFailed);
    } else if (hasChanges()) {
      updateAccount(caldavAccount.getName());
    } else {
      finish();
    }
  }

  private void addAccount(String name) {
    CaldavAccount newAccount = new CaldavAccount(name, UUIDHelper.newUUID());
    newAccount.setColor(selectedTheme);
    newAccount.setUrl(getNewURL());
    newAccount.setUsername(getNewUsername());
    newAccount.setPassword(getNewPassword());
    newAccount.setId(caldavDao.insert(newAccount));
    setResult(
        RESULT_OK,
        new Intent().putExtra(TaskListActivity.OPEN_FILTER, new CaldavFilter(newAccount)));
    finish();
  }

  private void updateAccount(String name) {
    caldavAccount.setName(name);
    caldavAccount.setUrl(getNewURL());
    caldavAccount.setUsername(getNewUsername());
    caldavAccount.setColor(selectedTheme);
    caldavAccount.setPassword(getNewPassword());
    caldavDao.update(caldavAccount);
    setResult(
        RESULT_OK,
        new Intent().putExtra(TaskListActivity.OPEN_FILTER, new CaldavFilter(caldavAccount)));
    finish();
  }

  private void getDisplayNameFailed(Throwable t) {
    if (t instanceof HttpException) {
      showSnackbar(t.getMessage());
    } else if (t instanceof DisplayableException) {
      showSnackbar(((DisplayableException) t).getResId());
    } else if (t instanceof ConnectException) {
      showSnackbar(R.string.network_error);
    } else {
      showGenericError();
    }
  }

  private void showGenericError() {
    showSnackbar(R.string.error_adding_account);
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
      return selectedTheme >= 0
          || !isEmpty(getNewPassword())
          || !isEmpty(getNewURL())
          || !isEmpty(getNewUsername());
    }
    return selectedTheme != caldavAccount.getColor() || needsValidation();
  }

  private boolean needsValidation() {
    return !getNewURL().equals(caldavAccount.getUrl())
        || !getNewUsername().equals(caldavAccount.getUsername())
        || !getNewPassword().equals(caldavAccount.getPassword());
  }

  @Override
  public void finish() {
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(url.getWindowToken(), 0);
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

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_COLOR_PICKER) {
      if (resultCode == RESULT_OK) {
        int index = data.getIntExtra(ColorPickerActivity.EXTRA_THEME_INDEX, 0);
        tracker.reportEvent(Tracking.Events.SET_TAG_COLOR, Integer.toString(index));
        selectedTheme = index;
        updateTheme();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void deleteAccount() {
    dialogBuilder
        .newMessageDialog(R.string.delete_tag_confirmation, caldavAccount.getName())
        .setPositiveButton(
            R.string.delete,
            (dialog, which) -> {
              if (caldavAccount != null) {
                caldavDao.delete(caldavAccount);
                setResult(
                    RESULT_OK,
                    new Intent(ACTION_DELETED)
                        .putExtra(EXTRA_CALDAV_UUID, caldavAccount.getUuid()));
              }
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

  private void updateTheme() {
    ThemeColor themeColor;
    if (selectedTheme < 0) {
      themeColor = this.themeColor;
      color.setText(R.string.none);
    } else {
      themeColor = themeCache.getThemeColor(selectedTheme);
      color.setText(themeColor.getName());
    }
    themeColor.apply(toolbar);
    themeColor.applyToStatusBar(this);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.delete:
        deleteAccount();
        break;
    }
    return super.onOptionsItemSelected(item);
  }
}
