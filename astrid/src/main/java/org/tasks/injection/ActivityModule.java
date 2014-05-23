package org.tasks.injection;

import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.activity.ShareLinkActivity;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.calls.MissedCallActivity;
import com.todoroo.astrid.core.CustomFilterActivity;
import com.todoroo.astrid.core.CustomFilterExposer;
import com.todoroo.astrid.gcal.CalendarAlarmListCreator;
import com.todoroo.astrid.gcal.CalendarReminderActivity;
import com.todoroo.astrid.gtasks.GtasksListAdder;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.tags.TagFilterExposer;

import org.tasks.voice.VoiceCommandActivity;

import dagger.Module;

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
                GtasksLoginActivity.class
        })
public class ActivityModule {
}
