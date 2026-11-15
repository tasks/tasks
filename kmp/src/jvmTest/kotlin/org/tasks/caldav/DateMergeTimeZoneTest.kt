package org.tasks.caldav

import org.junit.Test
import java.util.TimeZone

class DateMergeTimeZoneTest {
    private fun inEachTimeZone(block: () -> Unit) {
        val default = TimeZone.getDefault()
        try {
            for (id in ZONES) {
                TimeZone.setDefault(TimeZone.getTimeZone(id))
                try {
                    block()
                } catch (e: AssertionError) {
                    throw AssertionError("in $id: ${e.message}", e)
                }
            }
        } finally {
            TimeZone.setDefault(default)
        }
    }

    @Test
    fun unchangedCopiesKeepBothDates() = inEachTimeZone { DateMergeMatrix.unchangedCopiesKeepBothDates() }

    @Test
    fun unchangedCompletedCopiesKeepTheirCompletionDate() =
        inEachTimeZone { DateMergeMatrix.unchangedCompletedCopiesKeepTheirCompletionDate() }

    @Test
    fun copiesWrittenByTasksOnlyLoseMilliseconds() = inEachTimeZone { DateMergeMatrix.copiesWrittenByTasksOnlyLoseMilliseconds() }

    @Test
    fun pushingAnEditAndDownloadingAgainIsStable() = inEachTimeZone { DateMergeMatrix.pushingAnEditAndDownloadingAgainIsStable() }

    @Test
    fun localEditsKeepTheirModificationDate() = inEachTimeZone { DateMergeMatrix.localEditsKeepTheirModificationDate() }

    @Test
    fun localEditsLoseTheirModificationDateWhenTheCachedCopyHasNoLastModified() =
        inEachTimeZone { DateMergeMatrix.localEditsLoseTheirModificationDateWhenTheCachedCopyHasNoLastModified() }

    @Test
    fun modificationDateMovesUpToCreatedWhenLastModifiedIsOlder() =
        inEachTimeZone { DateMergeMatrix.modificationDateMovesUpToCreatedWhenLastModifiedIsOlder() }

    companion object {
        private val ZONES = listOf(
            "UTC",
            "America/New_York",
            "America/St_Johns",
            "Europe/Berlin",
            "Asia/Kolkata",
            "Australia/Lord_Howe",
            "Pacific/Chatham",
            "Pacific/Kiritimati",
            "America/Adak",
        )
    }
}
