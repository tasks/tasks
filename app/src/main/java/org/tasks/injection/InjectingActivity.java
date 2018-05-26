package org.tasks.injection;

public interface InjectingActivity {

  void inject(ActivityComponent component);

  ActivityComponent getComponent();
}
