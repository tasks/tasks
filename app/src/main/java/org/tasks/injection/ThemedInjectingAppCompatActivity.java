package org.tasks.injection;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import javax.inject.Inject;
import org.tasks.locale.Locale;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

public abstract class ThemedInjectingAppCompatActivity extends AppCompatActivity
    implements InjectingActivity {

  @Inject Theme theme;
  @Inject ThemeCache themeCache;
  @Inject protected ThemeColor themeColor;

  private ActivityComponent activityComponent;

  protected ThemedInjectingAppCompatActivity() {
    Locale.getInstance(this).applyOverrideConfiguration(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    activityComponent =
        ((InjectingApplication) getApplication()).getComponent().plus(new ActivityModule(this));
    inject(activityComponent);
    setTitle(null);
    theme.applyThemeAndStatusBarColor(this);
    super.onCreate(savedInstanceState);
  }

  @Override
  public ActivityComponent getComponent() {
    return activityComponent;
  }
}
