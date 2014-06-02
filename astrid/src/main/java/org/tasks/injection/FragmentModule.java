package org.tasks.injection;

import android.app.Activity;
import android.content.Context;

import com.todoroo.astrid.actfm.TagCommentsFragment;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.gtasks.GtasksListFragment;
import com.todoroo.astrid.reminders.NotificationFragment;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.tasks.injection.ActivityModule.ForActivity;
import static org.tasks.injection.TasksModule.ForApplication;

@Module(injects = {
        TaskListFragment.class,
        GtasksListFragment.class,
        NotificationFragment.class,
        SubtasksListFragment.class,
        SubtasksTagListFragment.class,
        TagViewFragment.class,
        TaskEditFragment.class,
        TagCommentsFragment.class,
        FilterListFragment.class
})
public class FragmentModule {

    private final Activity activity;

    public FragmentModule(Activity activity) {
        this.activity = activity;
    }

    @Singleton
    @Provides
    @ForApplication
    public Context getApplicationContext() {
        return activity.getApplicationContext();
    }

    @Singleton
    @Provides
    @ForActivity
    public Context getContext() {
        return activity;
    }
}
