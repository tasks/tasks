package org.tasks.injection;

import dagger.Subcomponent;

@ActivityScope
@Subcomponent(modules = ActivityModule.class)
public interface ActivityComponent extends BaseActivityComponent {

}
