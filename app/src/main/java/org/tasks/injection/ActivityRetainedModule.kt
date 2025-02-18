package org.tasks.injection

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.tasks.ui.TaskListEventBus

@Module
@InstallIn(ActivityRetainedComponent::class)
class ActivityRetainedModule {
    @Provides
    @ActivityRetainedScoped
    fun getTaskListBus(): TaskListEventBus = makeFlow()

    private fun <T> makeFlow() = MutableSharedFlow<T>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
}