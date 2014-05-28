package org.tasks.injection;

import com.todoroo.astrid.actfm.TagCommentsFragment;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.gtasks.GtasksListFragment;
import com.todoroo.astrid.reminders.NotificationFragment;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;

import dagger.Module;

@Module(library = true,
        injects = {
                TaskListFragment.class,
                GtasksListFragment.class,
                NotificationFragment.class,
                SubtasksListFragment.class,
                SubtasksTagListFragment.class,
                TagViewFragment.class,
                TaskEditFragment.class,
                TagCommentsFragment.class
        })
public class FragmentModule {
}
