package org.tasks.injection

import com.todoroo.astrid.activity.BeastModePreferences
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.ShareLinkActivity
import com.todoroo.astrid.activity.TaskEditActivity
import com.todoroo.astrid.gcal.CalendarReminderActivity
import com.todoroo.astrid.gtasks.auth.GtasksLoginActivity
import dagger.Subcomponent
import org.tasks.activities.*
import org.tasks.activities.attribution.AttributionActivity
import org.tasks.billing.PurchaseActivity
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.caldav.CaldavCalendarSettingsActivity
import org.tasks.caldav.LocalListSettingsActivity
import org.tasks.dashclock.DashClockSettings
import org.tasks.drive.DriveLoginActivity
import org.tasks.etesync.EncryptionSettingsActivity
import org.tasks.etesync.EteSyncAccountSettingsActivity
import org.tasks.etesync.EteSyncCalendarSettingsActivity
import org.tasks.locale.ui.activity.TaskerCreateTaskActivity
import org.tasks.locale.ui.activity.TaskerSettingsActivity
import org.tasks.location.LocationPickerActivity
import org.tasks.preferences.*
import org.tasks.reminders.NotificationActivity
import org.tasks.reminders.SnoozeActivity
import org.tasks.tags.TagPickerActivity
import org.tasks.tags.TagPickerViewModel
import org.tasks.ui.TaskListViewModel
import org.tasks.voice.VoiceCommandActivity
import org.tasks.widget.ShortcutConfigActivity
import org.tasks.widget.WidgetClickActivity
import org.tasks.widget.WidgetConfigActivity

@ActivityScope
@Subcomponent(modules = [ActivityModule::class, LocationModule::class])
interface ActivityComponent {
    operator fun plus(module: FragmentModule): FragmentComponent
    operator fun plus(dialogFragmentModule: DialogFragmentModule): DialogFragmentComponent
    fun inject(activity: GtasksLoginActivity)
    fun inject(activity: TaskerSettingsActivity)
    fun inject(activity: DashClockSettings)
    fun inject(activity: CalendarReminderActivity)
    fun inject(activity: FilterSettingsActivity)
    fun inject(activity: TagSettingsActivity)
    fun inject(activity: ShareLinkActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: BeastModePreferences)
    fun inject(activity: NotificationActivity)
    fun inject(activity: SnoozeActivity)
    fun inject(activity: FilterSelectionActivity)
    fun inject(activity: DateAndTimePickerActivity)
    fun inject(activity: CameraActivity)
    fun inject(activity: VoiceCommandActivity)
    fun inject(activity: WidgetConfigActivity)
    fun inject(activity: ShortcutConfigActivity)
    fun inject(activity: GoogleTaskListSettingsActivity)
    fun inject(activity: CaldavCalendarSettingsActivity)
    fun inject(activity: TaskerCreateTaskActivity)
    fun inject(activity: TaskListViewModel)
    fun inject(activity: PurchaseActivity)
    fun inject(activity: CaldavAccountSettingsActivity)
    fun inject(activity: EteSyncAccountSettingsActivity)
    fun inject(activity: DriveLoginActivity)
    fun inject(activity: TaskEditActivity)
    fun inject(activity: WidgetClickActivity)
    fun inject(activity: LocationPickerActivity)
    fun inject(activity: AttributionActivity)
    fun inject(activity: TagPickerActivity)
    fun inject(activity: TagPickerViewModel)
    fun inject(activity: EteSyncCalendarSettingsActivity)
    fun inject(activity: EncryptionSettingsActivity)
    fun inject(activity: MainPreferences)
    fun inject(activity: HelpAndFeedback)
    fun inject(activity: NotificationPreferences)
    fun inject(activity: ManageSpaceActivity)
    fun inject(activity: SyncPreferences)
    fun inject(activity: PlaceSettingsActivity)
    fun inject(activity: LocalListSettingsActivity)
    fun inject(activity: NavigationDrawerCustomization)
}