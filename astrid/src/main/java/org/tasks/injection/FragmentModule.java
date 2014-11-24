package org.tasks.injection;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;

import com.todoroo.astrid.actfm.TagViewFragment;
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

import org.tasks.ui.NavigationDrawerFragment;

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
        NavigationDrawerFragment.class,
        QuickAddBar.class,
        CoreFilterExposer.class,
        TimerFilterExposer.class,
        CustomFilterExposer.class,
        GtasksFilterExposer.class,
        TagFilterExposer.class
})
public class FragmentModule {

    private final Fragment fragment;
    private final Injector injector;

    public FragmentModule(Fragment fragment, Injector injector) {
        this.fragment = fragment;
        this.injector = injector;
    }

    @Singleton
    @Provides
    public Injector getInjector() {
        return injector;
    }

    @Provides
    @ForActivity
    public Context getContext() {
        return fragment.getActivity();
    }

    @Provides
    public Activity getActivity() {
        return fragment.getActivity();
    }

    @Singleton
    @Provides
    public Fragment getFragment() {
        return fragment;
    }
}
