package org.tasks.injection;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = TestModule.class)
public interface TestComponent extends BaseTestComponent {
}
