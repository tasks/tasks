package org.tasks;

import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.service.AstridDependencyInjector;

import org.tasks.widget.ScrollableViewsFactory;

import dagger.Module;

@Module(
        injects = {
                AstridDependencyInjector.class,
                ScrollableViewsFactory.class,
                FilterAdapter.class
        }
)
public class TasksModule {
}
