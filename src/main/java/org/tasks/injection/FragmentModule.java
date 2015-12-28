package org.tasks.injection;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;

import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.activity.TaskEditFragment;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.gtasks.GtasksListFragment;
import com.todoroo.astrid.subtasks.SubtasksListFragment;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;
import com.todoroo.astrid.ui.QuickAddBar;

import org.tasks.ui.NavigationDrawerFragment;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(addsTo = TasksModule.class,
        injects = {
                TaskListFragment.class,
                GtasksListFragment.class,
                SubtasksListFragment.class,
                SubtasksTagListFragment.class,
                TagViewFragment.class,
                TaskEditFragment.class,
                NavigationDrawerFragment.class,
                QuickAddBar.class
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
