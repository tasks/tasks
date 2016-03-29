package org.tasks.injection;

import dagger.Subcomponent;

@Subcomponent(modules = ServiceModule.class)
public interface ServiceComponent extends BaseServiceComponent {

}
