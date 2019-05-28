package org.tasks.preferences;

import static com.google.common.primitives.Ints.asList;

import android.os.Bundle;
import org.tasks.R;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;

public class DebugPreferences extends InjectingPreferenceActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_debug);

    for (int pref :
        asList(
            R.string.p_flipper,
            R.string.p_leak_canary,
            R.string.p_strict_mode_vm,
            R.string.p_strict_mode_thread)) {
      findPreference(pref)
          .setOnPreferenceChangeListener(
              (preference, newValue) -> {
                showRestartDialog();
                return true;
              });
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
