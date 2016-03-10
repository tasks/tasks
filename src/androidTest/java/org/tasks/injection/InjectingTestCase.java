package org.tasks.injection;

import android.test.AndroidTestCase;

import static org.tasks.TestUtilities.initializeMockito;

public abstract class InjectingTestCase extends AndroidTestCase {

    @Override
    protected void setUp() {
        initializeMockito(getContext());

        TestComponent component = DaggerTestComponent.builder()
                .testModule(new TestModule(getContext()))
                .build();
        inject(component);
    }

    protected abstract void inject(TestComponent component);
}
