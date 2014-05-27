package org.tasks;

import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.alarms.AlarmControlSet;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.gcal.GCalControlSet;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerActionControlSet;
import com.todoroo.astrid.timers.TimerFilterExposer;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.ui.QuickAddBar;

import org.tasks.widget.ScrollableViewsFactory;

import dagger.Module;

@Module(
        injects = {
                AstridDependencyInjector.class,
                ScrollableViewsFactory.class,
                QuickAddBar.class,
                EditTitleControlSet.class,
                FilesControlSet.class,
                TagsControlSet.class,
                AlarmControlSet.class,
                FilterAdapter.class,
                TimerFilterExposer.class,
                GCalControlSet.class,
                TimerActionControlSet.class
        }
)
public class TasksModule {
}
