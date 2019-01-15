package org.tasks.preferences;

import android.os.Bundle;
import org.tasks.R;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;

public class DebugPreferences extends InjectingPreferenceActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_debug);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
