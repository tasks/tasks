package org.tasks.viewmodel

import org.tasks.preferences.DatePickerPreferences
import org.tasks.viewmodel.DateAndTimeViewModel.OrderError
import org.tasks.viewmodel.DateAndTimeViewModel.QuickPickSlot
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.date_shortcut_afternoon
import tasks.kmp.generated.resources.date_shortcut_evening
import tasks.kmp.generated.resources.date_shortcut_morning
import tasks.kmp.generated.resources.date_shortcut_must_come_after
import tasks.kmp.generated.resources.date_shortcut_must_come_before
import tasks.kmp.generated.resources.date_shortcut_night
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuickPickTimeOrderTest {
    private val hour = 60 * 60 * 1000

    // 09:00, 13:00, 17:00, 20:00
    private val prefs = DatePickerPreferences(
        shortcutMorning = 9 * hour,
        shortcutAfternoon = 13 * hour,
        shortcutEvening = 17 * hour,
        shortcutNight = 20 * hour,
    )

    @Test
    fun acceptsTimeBetweenNeighbours() {
        assertNull(prefs.orderError(QuickPickSlot.AFTERNOON, 12 * hour))
        assertNull(prefs.orderError(QuickPickSlot.MORNING, 0))
        assertNull(prefs.orderError(QuickPickSlot.NIGHT, 23 * hour))
    }

    @Test
    fun rejectsTimeNotBeforeNext() {
        assertEquals(
            OrderError(
                Res.string.date_shortcut_must_come_before,
                Res.string.date_shortcut_morning,
                Res.string.date_shortcut_afternoon,
            ),
            prefs.orderError(QuickPickSlot.MORNING, 13 * hour),
        )
    }

    @Test
    fun rejectsTimeNotAfterPrevious() {
        assertEquals(
            OrderError(
                Res.string.date_shortcut_must_come_after,
                Res.string.date_shortcut_night,
                Res.string.date_shortcut_evening,
            ),
            prefs.orderError(QuickPickSlot.NIGHT, 17 * hour),
        )
    }

    @Test
    fun checksPreviousBeforeNext() {
        // Android checks "must come after" first; an evening before the afternoon reports that.
        assertEquals(
            OrderError(
                Res.string.date_shortcut_must_come_after,
                Res.string.date_shortcut_evening,
                Res.string.date_shortcut_afternoon,
            ),
            prefs.orderError(QuickPickSlot.EVENING, 8 * hour),
        )
    }

    @Test
    fun withTimeOnlyChangesThatSlot() {
        assertEquals(
            prefs.copy(shortcutEvening = 18 * hour),
            prefs.withTime(QuickPickSlot.EVENING, 18 * hour),
        )
    }
}
