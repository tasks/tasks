package org.tasks.injection;

import org.tasks.Tasks;

import dagger.Module;

@Module(injects = { Tasks.class })
public class TasksModule {
}
