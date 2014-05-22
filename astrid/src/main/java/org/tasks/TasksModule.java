package org.tasks;

import com.todoroo.astrid.service.AstridDependencyInjector;

import dagger.Module;

@Module(
        injects = {
                AstridDependencyInjector.class
        }
)
public class TasksModule {
}
