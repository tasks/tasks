package org.tasks.injection;

import org.junit.Before;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.tasks.TestUtilities.initializeMockito;

public abstract class InjectingTestCase {

    protected TestComponent component;

    @Before
    public void setUp() {
        initializeMockito(getTargetContext());

        component = DaggerTestComponent.builder()
                .testModule(new TestModule(getTargetContext()))
                .build();
        inject(component);
    }

    protected abstract void inject(TestComponent component);
}
