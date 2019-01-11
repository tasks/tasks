package org.tasks.injection;

import org.junit.Before;

import timber.log.Timber;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static org.tasks.TestUtilities.initializeMockito;

public abstract class InjectingTestCase {

  @Before
  public void setUp() {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> Timber.e(e));

    initializeMockito(getTargetContext());

    TestComponent component =
        DaggerTestComponent.builder().testModule(new TestModule(getTargetContext())).build();
    inject(component);
  }

  protected abstract void inject(TestComponent component);
}
