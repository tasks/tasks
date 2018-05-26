package org.tasks.injection;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import org.tasks.locale.Locale;

public abstract class InjectingAppCompatActivity extends AppCompatActivity
    implements InjectingActivity {

  private ActivityComponent activityComponent;

  protected InjectingAppCompatActivity() {
    Locale.getInstance(this).applyOverrideConfiguration(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    activityComponent =
        ((InjectingApplication) getApplication()).getComponent().plus(new ActivityModule(this));
    inject(activityComponent);
    setTitle("");
    super.onCreate(savedInstanceState);
  }

  @Override
  public ActivityComponent getComponent() {
    return activityComponent;
  }
}
