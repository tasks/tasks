package org.tasks.injection

import dagger.Subcomponent
import org.tasks.activities.ListPicker
import org.tasks.calendars.CalendarPicker
import org.tasks.dialogs.*
import org.tasks.locale.LocalePickerDialog
import org.tasks.reminders.NotificationDialog
import org.tasks.reminders.SnoozeDialog
import org.tasks.repeats.BasicRecurrenceDialog
import org.tasks.repeats.CustomRecurrenceDialog

@Subcomponent(modules = [DialogFragmentModule::class])
interface DialogFragmentComponent {
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