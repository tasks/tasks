package org.tasks.injection;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import javax.inject.Inject;
import org.tasks.locale.Locale;
import org.tasks.themes.Theme;

public abstract class ThemedInjectingAppCompatActivity extends AppCompatActivity
    implements InjectingActivity {

  @Inject Theme theme;
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
    super.onCreate(savedInstanceState);
    theme.applyTheme(this);
    theme.applyStatusBarColor(this);
  }

  @Override
  public ActivityComponent getComponent() {
    return activityComponent;
  }
}
