package org.tasks.injection;

import dagger.Subcomponent;

@Subcomponent(modules = BroadcastModule.class)
public interface BroadcastComponent extends BaseBroadcastComponent {

}
