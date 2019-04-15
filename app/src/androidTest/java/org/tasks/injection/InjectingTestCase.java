package org.tasks.injection;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.tasks.TestUtilities.initializeMockito;

import android.content.Context;
import org.junit.Before;
import timber.log.Timber;

public abstract class InjectingTestCase {

  @Before
  public void setUp() {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> Timber.e(e));

    Context context = getApplicationContext();

    initializeMockito(context);

    TestComponent component =
        DaggerTestComponent.builder()
            .applicationModule(new ApplicationModule(context))
            .testModule(new TestModule()).build();
    inject(component);
  }

  protected abstract void inject(TestComponent component);
}
