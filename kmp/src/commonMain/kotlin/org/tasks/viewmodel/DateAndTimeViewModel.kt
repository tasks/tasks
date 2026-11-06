package org.tasks.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DatePickerPreferences
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.date_shortcut_afternoon
import tasks.kmp.generated.resources.date_shortcut_evening
import tasks.kmp.generated.resources.date_shortcut_morning
import tasks.kmp.generated.resources.date_shortcut_must_come_after
import tasks.kmp.generated.resources.date_shortcut_must_come_before
import tasks.kmp.generated.resources.date_shortcut_night

class DateAndTimeViewModel(
    private val appPreferences: AppPreferences,
    persistenceScope: CoroutineScope,
) : ViewModel() {

    /** In the order the times must keep. */
    enum class QuickPickSlot(val label: StringResource) {
        MORNING(Res.string.date_shortcut_morning),
        AFTERNOON(Res.string.date_shortcut_afternoon),
        EVENING(Res.string.date_shortcut_evening),
        NIGHT(Res.string.date_shortcut_night),
    }

    data class OrderError(
        val message: StringResource,
        val slot: StringResource,
        val other: StringResource,
    )

    var preferences by mutableStateOf(DatePickerPreferences())
        private set

    var loaded by mutableStateOf(false)
        private set

    var timePickerSlot by mutableStateOf<QuickPickSlot?>(null)
        private set

    var orderError by mutableStateOf<OrderError?>(null)
        private set

    private val writes = PreferenceWriteQueue(
        viewModelScope = viewModelScope,
        persistenceScope = persistenceScope,
        tag = TAG,
        reload = { reloadSafely() },
    )

    init {
        viewModelScope.launch {
            reloadSafely()
        }
    }

    private suspend fun reloadSafely() {
        try {
            preferences = appPreferences.datePickerPreferences()
            loaded = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e, tag = TAG) { "Failed to reload date and time settings" }
        }
    }

    fun updateFullDate(enabled: Boolean) {
        preferences = preferences.copy(alwaysDisplayFullDate = enabled)
        writes.write { appPreferences.setAlwaysDisplayFullDate(enabled) }
    }

    fun openTimePicker(slot: QuickPickSlot) {
        timePickerSlot = slot
    }

    fun dismissTimePicker() {
        timePickerSlot = null
    }

    fun dismissOrderError() {
        orderError = null
    }

    fun setTime(slot: QuickPickSlot, millisOfDay: Int) {
        timePickerSlot = null
        orderError = preferences.orderError(slot, millisOfDay)
        if (orderError != null) return
        preferences = preferences.withTime(slot, millisOfDay)
        val times = preferences.quickPickTimes
        writes.write { appPreferences.setQuickPickTimes(times) }
    }

    fun updateTimePickerInputMode(inputMode: Boolean) {
        preferences = preferences.copy(timePickerInputMode = inputMode)
        writes.write { appPreferences.setTimePickerInputMode(inputMode) }
    }
}

fun DatePickerPreferences.time(slot: DateAndTimeViewModel.QuickPickSlot): Int = when (slot) {
    DateAndTimeViewModel.QuickPickSlot.MORNING -> shortcutMorning
    DateAndTimeViewModel.QuickPickSlot.AFTERNOON -> shortcutAfternoon
    DateAndTimeViewModel.QuickPickSlot.EVENING -> shortcutEvening
    DateAndTimeViewModel.QuickPickSlot.NIGHT -> shortcutNight
}

internal fun DatePickerPreferences.withTime(
    slot: DateAndTimeViewModel.QuickPickSlot,
    millisOfDay: Int,
): DatePickerPreferences = when (slot) {
    DateAndTimeViewModel.QuickPickSlot.MORNING -> copy(shortcutMorning = millisOfDay)
    DateAndTimeViewModel.QuickPickSlot.AFTERNOON -> copy(shortcutAfternoon = millisOfDay)
    DateAndTimeViewModel.QuickPickSlot.EVENING -> copy(shortcutEvening = millisOfDay)
    DateAndTimeViewModel.QuickPickSlot.NIGHT -> copy(shortcutNight = millisOfDay)
}

/** Same rule as Android's DateAndTimeViewModel: each time must fall between its neighbours. */
internal fun DatePickerPreferences.orderError(
    slot: DateAndTimeViewModel.QuickPickSlot,
    millisOfDay: Int,
): DateAndTimeViewModel.OrderError? {
    val slots = DateAndTimeViewModel.QuickPickSlot.entries
    val previous = slots.getOrNull(slot.ordinal - 1)
    val next = slots.getOrNull(slot.ordinal + 1)
    return when {
        previous != null && millisOfDay <= time(previous) -> DateAndTimeViewModel.OrderError(
            Res.string.date_shortcut_must_come_after, slot.label, previous.label,
        )
        next != null && millisOfDay >= time(next) -> DateAndTimeViewModel.OrderError(
            Res.string.date_shortcut_must_come_before, slot.label, next.label,
        )
        else -> null
    }
}

private const val TAG = "DateAndTimeViewModel"
