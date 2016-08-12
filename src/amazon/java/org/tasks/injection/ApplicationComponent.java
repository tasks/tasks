package org.tasks.injection;

import dagger.Component;

@ApplicationScope
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent extends BaseApplicationComponent {
}
