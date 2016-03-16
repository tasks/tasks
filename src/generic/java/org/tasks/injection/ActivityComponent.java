package org.tasks.injection;

import javax.inject.Singleton;

import dagger.Subcomponent;

@Singleton
@Subcomponent(modules = ActivityModule.class)
public interface ActivityComponent extends BaseActivityComponent {

}
