package org.tasks.injection;

import android.app.Activity;
import android.content.Context;

import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.activity.EditPreferences;
import com.todoroo.astrid.activity.ShareLinkActivity;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.calls.MissedCallActivity;
import com.todoroo.astrid.core.CustomFilterActivity;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.core.OldTaskPreferences;
import com.todoroo.astrid.gcal.CalendarAlarmListCreator;
import com.todoroo.astrid.gcal.CalendarReminderActivity;
import com.todoroo.astrid.gtasks.GtasksListAdder;
import com.todoroo.astrid.gtasks.GtasksPreferences;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.widget.WidgetConfigActivity;

import org.tasks.voice.VoiceCommandActivity;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.tasks.injection.TasksModule.ForApplication;

@Module(library = true,
        injects = {
                TaskListActivity.class,
                TaskEditActivity.class,
                ShareLinkActivity.class,
                TagSettingsActivity.class,
                CustomFilterActivity.class,
                MissedCallActivity.class,
                CalendarAlarmListCreator.class,
                CustomFilterExposer.DeleteActivity.class,
                CalendarReminderActivity.class,
                GtasksListAdder.class,
                TagFilterExposer.DeleteTagActivity.class,
                TagFilterExposer.RenameTagActivity.class,
                VoiceCommandActivity.class,
                GtasksLoginActivity.class,
                WidgetConfigActivity.class,
                EditPreferences.class,
                GtasksPreferences.class,
                OldTaskPreferences.class
        })
public class ActivityModule {

    private final Context context;

    public ActivityModule(Activity activity) {
        context = activity.getApplicationContext();
    }

    @Singleton
    @Provides
    @ForApplication
    public Context getApplicationContext() {
        return context;
    }

    @Qualifier
    @Target({FIELD, PARAMETER, METHOD})
    @Documented
    @Retention(RUNTIME)
    public @interface ForActivity {
    }
}
