package org.tasks.injection

import com.todoroo.astrid.activity.TaskEditFragment
import com.todoroo.astrid.activity.TaskListFragment
import com.todoroo.astrid.files.FilesControlSet
import com.todoroo.astrid.repeats.RepeatControlSet
import com.todoroo.astrid.tags.TagsControlSet
import com.todoroo.astrid.timers.TimerControlSet
import com.todoroo.astrid.ui.HideUntilControlSet
import com.todoroo.astrid.ui.ReminderControlSet
import dagger.Subcomponent
import org.tasks.activities.ListPicker
import org.tasks.calendars.CalendarPicker
import org.tasks.dialogs.*
import org.tasks.fragments.CommentBarFragment
import org.tasks.locale.LocalePickerDialog
import org.tasks.preferences.fragments.*
import org.tasks.reminders.NotificationDialog
import org.tasks.reminders.SnoozeDialog
import org.tasks.repeats.BasicRecurrenceDialog
import org.tasks.repeats.CustomRecurrenceDialog
import org.tasks.ui.*

@Subcomponent(modules = [FragmentModule::class])
interface FragmentComponent {
    fun inject(fragment: TimerControlSet)
    fun inject(fragment: TaskEditFragment)
    fun inject(fragment: NavigationDrawerFragment)
    fun inject(fragment: PriorityControlSet)
    fun inject(fragment: RepeatControlSet)
    fun inject(fragment: CommentBarFragment)
    fun inject(fragment: FilesControlSet)
    fun inject(fragment: TagsControlSet)
    fun inject(fragment: HideUntilControlSet)
    fun inject(fragment: ReminderControlSet)
    fun inject(fragment: DeadlineControlSet)
    fun inject(fragment: DescriptionControlSet)
    fun inject(fragment: CalendarControlSet)
    fun inject(fragment: TaskListFragment)
    fun inject(fragment: ListFragment)
    fun inject(fragment: LocationControlSet)
    fun inject(fragment: SubtaskControlSet)
    fun inject(fragment: TaskListViewModel)
    fun inject(fragment: HelpAndFeedback)
    fun inject(fragment: LookAndFeel)
    fun inject(fragment: Synchronization)
    fun inject(fragment: Debug)
    fun inject(fragment: MainSettingsFragment)
    fun inject(fragment: Backups)
    fun inject(fragment: Advanced)
    fun inject(fragment: Notifications)
    fun inject(fragment: TaskDefaults)
    fun inject(fragment: ScrollableWidget)
    fun inject(fragment: DashClock)
    fun inject(fragment: TaskerListNotification)
    fun inject(fragment: EmptyTaskEditFragment)
    fun inject(fragment: NavigationDrawer)
    fun inject(fragment: Widgets)
    fun inject(fragment: DateAndTime)

    fun inject(dialogFragment: ListPicker)
    fun inject(dialogFragment: NotificationDialog)
    fun inject(dialogFragment: CalendarPicker)
    fun inject(dialogFragment: AddAttachmentDialog)
    fun inject(dialogFragment: SnoozeDialog)
    fun inject(dialogFragment: SortDialog)
    fun inject(dialogFragment: RecordAudioDialog)
    fun inject(dialogFragment: CustomRecurrenceDialog)
    fun inject(dialogFragment: BasicRecurrenceDialog)
    fun inject(dialogFragment: GeofenceDialog)
    fun inject(dialogFragment: IconPickerDialog)
    fun inject(dialogFragment: ExportTasksDialog)
    fun inject(dialogFragment: ImportTasksDialog)
    fun inject(dialogFragment: LocalePickerDialog)
    fun inject(dialogFragment: ThemePickerDialog)
    fun inject(dialogFragment: ColorWheelPicker)
    fun inject(dialogFragment: ColorPalettePicker)
    fun inject(dialogFragment: DateTimePicker)
    fun inject(dialogFragment: NewFilterDialog)
    fun inject(dialogFragment: WhatsNewDialog)
}