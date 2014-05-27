package org.tasks;

import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.service.AstridDependencyInjector;

import dagger.Module;

@Module(
        injects = {
                AstridDependencyInjector.class,
                FilterAdapter.class
        }
)
public class TasksModule {
}
