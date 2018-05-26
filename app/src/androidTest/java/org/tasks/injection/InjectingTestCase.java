package org.tasks.injection;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.tasks.TestUtilities.initializeMockito;

import org.junit.Before;

public abstract class InjectingTestCase {

  private TestComponent component;

  @Before
  public void setUp() {
    initializeMockito(getTargetContext());

    component =
        DaggerTestComponent.builder().testModule(new TestModule(getTargetContext())).build();
    inject(component);
  }

  protected abstract void inject(TestComponent component);
}
