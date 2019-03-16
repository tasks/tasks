package org.tasks.injection;

import org.junit.Before;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static org.tasks.TestUtilities.initializeMockito;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.tasks.jobs.WorkManager;

public abstract class InjectingTestCase {

  private TestComponent component;
  private WorkManager workManager;

  @Before
  public void setUp() {
    initializeMockito(getTargetContext());
    workManager = mock(WorkManager.class);

    component =
        DaggerTestComponent.builder().testModule(new TestModule(getTargetContext(), workManager)).build();
    inject(component);
  }

  protected abstract void inject(TestComponent component);
}
