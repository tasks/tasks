package org.tasks.injection;

import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.activity.ShareLinkActivity;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.core.CustomFilterActivity;

import dagger.Module;

@Module(library = true,
        injects = {
                TaskListActivity.class,
                TaskEditActivity.class,
                ShareLinkActivity.class,
                TagSettingsActivity.class,
                CustomFilterActivity.class
        })
public class ActivityModule {
}
