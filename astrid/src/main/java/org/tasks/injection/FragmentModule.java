package org.tasks.injection;

import android.app.Activity;
import android.content.Context;

import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.gtasks.GtasksFilterExposer;
import com.todoroo.astrid.gtasks.GtasksListFragment;
import com.todoroo.astrid.reminders.NotificationFragment;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.timers.TimerFilterExposer;
import com.todoroo.astrid.ui.QuickAddBar;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(addsTo = TasksModule.class,
        injects = {
        TaskListFragment.class,
        GtasksListFragment.class,
        NotificationFragment.class,
        SubtasksListFragment.class,
        SubtasksTagListFragment.class,
        TagViewFragment.class,
        TaskEditFragment.class,
        FilterListFragment.class,
        QuickAddBar.class,
        CoreFilterExposer.class,
        TimerFilterExposer.class,
        CustomFilterExposer.class,
        GtasksFilterExposer.class,
        TagFilterExposer.class
})
public class FragmentModule {

    private final Activity activity;
    private Injector injector;

    public FragmentModule(Activity activity, Injector injector) {
        this.activity = activity;
        this.injector = injector;
    }

    @Singleton
    @Provides
    public Injector getInjector() {
        return injector;
    }

    @Singleton
    @Provides
    @ForActivity
    public Context getContext() {
        return activity;
    }

    @Singleton
    @Provides
    public Activity getActivity() {
        return activity;
    }
}
