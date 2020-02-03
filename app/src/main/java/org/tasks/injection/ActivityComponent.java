package org.tasks.injection;

import com.todoroo.astrid.activity.BeastModePreferences;
import com.todoroo.astrid.activity.MainActivity;
import com.todoroo.astrid.activity.ShareLinkActivity;
import com.todoroo.astrid.activity.TaskEditActivity;
import com.todoroo.astrid.core.CustomFilterActivity;
import com.todoroo.astrid.core.DefaultsPreferences;
import com.todoroo.astrid.core.OldTaskPreferences;
import com.todoroo.astrid.gcal.CalendarReminderActivity;
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity;
import com.todoroo.astrid.reminders.ReminderPreferences;
import dagger.Subcomponent;
import org.tasks.activities.CalendarSelectionActivity;
import org.tasks.activities.CameraActivity;
import org.tasks.activities.ColorPickerActivity;
import org.tasks.activities.DateAndTimePickerActivity;
import org.tasks.activities.DatePickerActivity;
import org.tasks.activities.FilterSelectionActivity;
import org.tasks.activities.FilterSettingsActivity;
import org.tasks.activities.GoogleTaskListSettingsActivity;
import org.tasks.etesync.EteSyncAccountSettingsActivity;
import org.tasks.tags.TagPickerActivity;
import org.tasks.activities.TagSettingsActivity;
import org.tasks.activities.TimePickerActivity;
import org.tasks.billing.PurchaseActivity;
import org.tasks.caldav.CaldavAccountSettingsActivity;
import org.tasks.caldav.CaldavCalendarSettingsActivity;
import org.tasks.dashclock.DashClockSettings;
import org.tasks.drive.DriveLoginActivity;
import org.tasks.files.FileExplore;
import org.tasks.files.MyFilePickerActivity;
import org.tasks.locale.ui.activity.TaskerCreateTaskActivity;
import org.tasks.locale.ui.activity.TaskerSettingsActivity;
import org.tasks.location.LocationPickerActivity;
import org.tasks.preferences.AttributionActivity;
import org.tasks.preferences.BasicPreferences;
import org.tasks.preferences.DateTimePreferences;
import org.tasks.preferences.DebugPreferences;
import org.tasks.preferences.MiscellaneousPreferences;
import org.tasks.reminders.NotificationActivity;
import org.tasks.reminders.SnoozeActivity;
import org.tasks.sync.SynchronizationPreferences;
import org.tasks.tags.TagPickerViewModel;
import org.tasks.themes.Theme;
import org.tasks.ui.TaskListViewModel;
import org.tasks.voice.VoiceCommandActivity;
import org.tasks.widget.ShortcutConfigActivity;
import org.tasks.widget.WidgetClickActivity;
import org.tasks.widget.WidgetConfigActivity;

@ActivityScope
@Subcomponent(modules = {ActivityModule.class, LocationModule.class})
public interface ActivityComponent {

  void inject(SynchronizationPreferences synchronizationPreferences);

  void inject(GtasksLoginActivity gtasksLoginActivity);

  Theme getTheme();

  FragmentComponent plus(FragmentModule module);

  DialogFragmentComponent plus(DialogFragmentModule dialogFragmentModule);

  NativeDialogFragmentComponent plus(NativeDialogFragmentModule nativeDialogFragmentModule);

  void inject(TaskerSettingsActivity taskerSettingsActivity);

  void inject(DashClockSettings dashClockSettings);

  void inject(CustomFilterActivity customFilterActivity);

  void inject(CalendarReminderActivity calendarReminderActivity);

  void inject(FilterSettingsActivity filterSettingsActivity);

  void inject(TagSettingsActivity tagSettingsActivity);

  void inject(ShareLinkActivity shareLinkActivity);

  void inject(MainActivity mainActivity);

  void inject(BeastModePreferences beastModePreferences);

  void inject(NotificationActivity notificationActivity);

  void inject(SnoozeActivity snoozeActivity);

  void inject(FileExplore fileExplore);

  void inject(CalendarSelectionActivity calendarSelectionActivity);

  void inject(FilterSelectionActivity filterSelectionActivity);

  void inject(DateAndTimePickerActivity dateAndTimePickerActivity);

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

  void inject(DateTimePreferences dateTimePreferences);

  void inject(MyFilePickerActivity myFilePickerActivity);

  void inject(ColorPickerActivity colorPickerActivity);

  void inject(BasicPreferences basicPreferences);

  void inject(GoogleTaskListSettingsActivity googleTaskListSettingsActivity);

  void inject(CaldavCalendarSettingsActivity caldavCalendarSettingsActivity);

  void inject(TaskerCreateTaskActivity taskerCreateTaskActivity);

  void inject(TaskListViewModel taskListViewModel);

  void inject(PurchaseActivity purchaseActivity);

  void inject(CaldavAccountSettingsActivity caldavAccountSettingsActivity);

  void inject(EteSyncAccountSettingsActivity eteSyncAccountSettingsActivity);

  void inject(DriveLoginActivity driveLoginActivity);

  void inject(DebugPreferences debugPreferences);

  void inject(TaskEditActivity taskEditActivity);

  void inject(WidgetClickActivity widgetActivity);

  void inject(LocationPickerActivity locationPickerActivity);

  void inject(AttributionActivity attributionActivity);

  void inject(TagPickerActivity tagPickerActivity);

  void inject(TagPickerViewModel viewModel);
}
