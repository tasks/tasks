package org.tasks.injection;

import android.app.Activity;

import com.todoroo.astrid.actfm.FilterSettingsActivity;
import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.activity.FilterShortcutActivity;
import com.todoroo.astrid.activity.ShareLinkActivity;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.core.CustomFilterActivity;
import com.todoroo.astrid.core.DefaultsPreferences;
import com.todoroo.astrid.core.OldTaskPreferences;
import com.todoroo.astrid.files.AACRecordingActivity;
import com.todoroo.astrid.files.FileExplore;
import com.todoroo.astrid.gcal.CalendarAlarmListCreator;
import com.todoroo.astrid.gcal.CalendarReminderActivity;
import com.todoroo.astrid.gtasks.GtasksPreferences;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.reminders.ReminderPreferences;
import com.todoroo.astrid.service.UpgradeActivity;
import com.todoroo.astrid.widget.WidgetConfigActivity;

import org.tasks.activities.AddAttachmentActivity;
import org.tasks.activities.CalendarSelectionActivity;
import org.tasks.activities.CameraActivity;
import org.tasks.activities.ClearAllDataActivity;
import org.tasks.activities.ClearGtaskDataActivity;
import org.tasks.activities.DateAndTimePickerActivity;
import org.tasks.activities.DeleteAllCalendarEventsActivity;
import org.tasks.activities.DeleteCompletedActivity;
import org.tasks.activities.DeleteCompletedEventsActivity;
import org.tasks.activities.DonationActivity;
import org.tasks.activities.ExportTaskActivity;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.activities.ImportTaskActivity;
import org.tasks.activities.PurgeDeletedActivity;
import org.tasks.activities.ResetPreferencesActivity;
import org.tasks.activities.SortActivity;
import org.tasks.activities.TimePickerActivity;
import org.tasks.preferences.AppearancePreferences;
import org.tasks.preferences.BackupPreferences;
import org.tasks.preferences.BasicPreferences;
import org.tasks.preferences.DateShortcutPreferences;
import org.tasks.preferences.HelpAndFeedbackActivity;
import org.tasks.preferences.MiscellaneousPreferences;
import org.tasks.reminders.MissedCallActivity;
import org.tasks.reminders.NotificationActivity;
import org.tasks.reminders.SnoozeActivity;
import org.tasks.voice.VoiceCommandActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(addsTo = TasksModule.class,
        injects = {
                TaskListActivity.class,
                TaskEditActivity.class,
                ShareLinkActivity.class,
                TagSettingsActivity.class,
                FilterSettingsActivity.class,
                CustomFilterActivity.class,
                MissedCallActivity.class,
                CalendarAlarmListCreator.class,
                CalendarReminderActivity.class,
                VoiceCommandActivity.class,
                GtasksLoginActivity.class,
                WidgetConfigActivity.class,
                BasicPreferences.class,
                GtasksPreferences.class,
                OldTaskPreferences.class,
                FilterShortcutActivity.class,
                BeastModePreferences.class,
                DefaultsPreferences.class,
                AACRecordingActivity.class,
                SnoozeActivity.class,
                MiscellaneousPreferences.class,
                ImportTaskActivity.class,
                ExportTaskActivity.class,
                ClearAllDataActivity.class,
                ResetPreferencesActivity.class,
                PurgeDeletedActivity.class,
                DeleteCompletedActivity.class,
                DeleteCompletedEventsActivity.class,
                DeleteAllCalendarEventsActivity.class,
                ClearGtaskDataActivity.class,
                ReminderPreferences.class,
                AppearancePreferences.class,
                BackupPreferences.class,
                NotificationActivity.class,
                HelpAndFeedbackActivity.class,
                DateShortcutPreferences.class,
                SortActivity.class,
                FilterSelectionActivity.class,
                FileExplore.class,
                DonationActivity.class,
                UpgradeActivity.class,
                CalendarSelectionActivity.class,
                AddAttachmentActivity.class,
                ShortcutActivity.class,
                CameraActivity.class,
                TimePickerActivity.class,
                DateAndTimePickerActivity.class
        })
public class ActivityModule {

    private final Activity activity;

    public ActivityModule(Activity activity) {
        this.activity = activity;
    }

    @Singleton
    @Provides
    public Activity getActivity() {
        return activity;
    }
}
