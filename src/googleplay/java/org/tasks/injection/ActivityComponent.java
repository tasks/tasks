package org.tasks.injection;

import com.todoroo.astrid.gtasks.GtasksPreferences;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;

import org.tasks.dashclock.DashClockSettings;
import org.tasks.locale.ui.activity.TaskerSettingsActivity;

import javax.inject.Singleton;

import dagger.Subcomponent;

@Singleton
@Subcomponent(modules = ActivityModule.class)
public interface ActivityComponent extends BaseActivityComponent {
    void inject(GtasksPreferences gtasksPreferences);

    void inject(TaskerSettingsActivity taskerSettingsActivity);

    void inject(GtasksLoginActivity gtasksLoginActivity);

    void inject(DashClockSettings dashClockSettings);
}
