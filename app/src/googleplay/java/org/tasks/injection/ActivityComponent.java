package org.tasks.injection;

import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.activity.ShareLinkActivity;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.core.CustomFilterActivity;
import com.todoroo.astrid.core.DefaultsPreferences;
import com.todoroo.astrid.core.OldTaskPreferences;
import com.todoroo.astrid.files.AACRecordingActivity;
import com.todoroo.astrid.gcal.CalendarReminderActivity;
import com.todoroo.astrid.gtasks.GtasksPreferences;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.reminders.ReminderPreferences;

import org.tasks.activities.AddAttachmentActivity;
import org.tasks.activities.CalendarSelectionActivity;
import org.tasks.activities.CameraActivity;
import org.tasks.activities.ColorPickerActivity;
import org.tasks.activities.DateAndTimePickerActivity;
import org.tasks.activities.DatePickerActivity;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.activities.FilterSettingsActivity;
import org.tasks.activities.GoogleTaskListSettingsActivity;
import org.tasks.activities.TagSettingsActivity;
import org.tasks.activities.TimePickerActivity;
import org.tasks.dashclock.DashClockSettings;
import org.tasks.files.FileExplore;
import org.tasks.files.MyFilePickerActivity;
import org.tasks.locale.ui.activity.TaskerSettingsActivity;
import org.tasks.preferences.AppearancePreferences;
import org.tasks.preferences.BasicPreferences;
import org.tasks.preferences.DateTimePreferences;
import org.tasks.preferences.HelpAndFeedbackActivity;
import org.tasks.preferences.MiscellaneousPreferences;
import org.tasks.reminders.MissedCallActivity;
import org.tasks.reminders.NotificationActivity;
import org.tasks.reminders.SnoozeActivity;
import org.tasks.themes.Theme;
import org.tasks.voice.VoiceCommandActivity;
import org.tasks.widget.ShortcutConfigActivity;
import org.tasks.widget.WidgetConfigActivity;

import dagger.Subcomponent;

@ActivityScope
@Subcomponent(modules = ActivityModule.class)
public interface ActivityComponent {

    void inject(GtasksPreferences gtasksPreferences);

    void inject(GtasksLoginActivity gtasksLoginActivity);

    Theme getTheme();

    FragmentComponent plus(FragmentModule module);

    DialogFragmentComponent plus(DialogFragmentModule dialogFragmentModule);

    NativeDialogFragmentComponent plus(NativeDialogFragmentModule nativeDialogFragmentModule);

    void inject(TaskerSettingsActivity taskerSettingsActivity);

    void inject(DashClockSettings dashClockSettings);

    void inject(AACRecordingActivity aacRecordingActivity);

    void inject(CustomFilterActivity customFilterActivity);

    void inject(CalendarReminderActivity calendarReminderActivity);

    void inject(FilterSettingsActivity filterSettingsActivity);

    void inject(TagSettingsActivity tagSettingsActivity);

    void inject(ShareLinkActivity shareLinkActivity);

    void inject(TaskListActivity taskListActivity);

    void inject(BeastModePreferences beastModePreferences);

    void inject(NotificationActivity notificationActivity);

    void inject(SnoozeActivity snoozeActivity);

    void inject(MissedCallActivity missedCallActivity);

    void inject(FileExplore fileExplore);

    void inject(CalendarSelectionActivity calendarSelectionActivity);

    void inject(FilterSelectionActivity filterSelectionActivity);

    void inject(DateAndTimePickerActivity dateAndTimePickerActivity);

    void inject(AddAttachmentActivity addAttachmentActivity);

    void inject(DatePickerActivity datePickerActivity);

    void inject(CameraActivity cameraActivity);

    void inject(TimePickerActivity timePickerActivity);

    void inject(VoiceCommandActivity voiceCommandActivity);

    void inject(ReminderPreferences reminderPreferences);

    void inject(WidgetConfigActivity widgetConfigActivity);

    void inject(OldTaskPreferences oldTaskPreferences);

    void inject(DefaultsPreferences defaultsPreferences);

    void inject(ShortcutConfigActivity shortcutConfigActivity);

    void inject(MiscellaneousPreferences miscellaneousPreferences);

    void inject(HelpAndFeedbackActivity helpAndFeedbackActivity);

    void inject(DateTimePreferences dateTimePreferences);

    void inject(AppearancePreferences appearancePreferences);

    void inject(MyFilePickerActivity myFilePickerActivity);

    void inject(ColorPickerActivity colorPickerActivity);

    void inject(BasicPreferences basicPreferences);

    void inject(GoogleTaskListSettingsActivity googleTaskListSettingsActivity);
}
