package org.tasks.injection;

import dagger.Component;

@ApplicationScope
@Component(modules = TestModule.class)
public interface TestComponent extends BaseTestComponent {
}
