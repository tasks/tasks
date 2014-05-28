package org.tasks.injection;

import com.todoroo.astrid.adapter.FilterAdapter;
import com.todoroo.astrid.alarms.AlarmControlSet;
import com.todoroo.astrid.backup.TasksXmlExporter;
import com.todoroo.astrid.backup.TasksXmlImporter;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.files.FilesControlSet;
import com.todoroo.astrid.gcal.GCalControlSet;
import com.todoroo.astrid.gtasks.GtasksFilterExposer;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagsControlSet;
import com.todoroo.astrid.timers.TimerActionControlSet;
import com.todoroo.astrid.timers.TimerFilterExposer;
import com.todoroo.astrid.ui.EditTitleControlSet;
import com.todoroo.astrid.ui.QuickAddBar;

import org.tasks.widget.ScrollableViewsFactory;

import dagger.Module;

@Module(
        injects = {
                ScrollableViewsFactory.class,
                QuickAddBar.class,
                EditTitleControlSet.class,
                FilesControlSet.class,
                TagsControlSet.class,
                AlarmControlSet.class,
                FilterAdapter.class,
                TimerFilterExposer.class,
                GCalControlSet.class,
                TimerActionControlSet.class,
                CustomFilterExposer.class,
                GtasksFilterExposer.class,
                TagFilterExposer.class,
                TasksXmlExporter.class,
                TasksXmlImporter.class
        }
)
public class TasksModule {
}
