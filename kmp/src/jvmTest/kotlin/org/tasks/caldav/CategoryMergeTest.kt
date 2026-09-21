package org.tasks.caldav

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryMergeTest {
    @Test
    fun allEmpty() {
        assertEquals(emptyList<String>(), mergeCategories(base = listOf(), local = listOf(), remote = listOf()))
    }

    @Test
    fun unchangedKeepsRemote() {
        assertEquals(
            listOf("A"),
            mergeCategories(base = listOf("A"), local = listOf("A"), remote = listOf("A")),
        )
    }

    @Test
    fun remoteAdditionApplied() {
        assertEquals(
            listOf("A", "B"),
            mergeCategories(base = listOf("A"), local = listOf("A"), remote = listOf("A", "B")),
        )
    }

    @Test
    fun localAdditionKept() {
        assertEquals(
            listOf("A", "B"),
            mergeCategories(base = listOf("A"), local = listOf("A", "B"), remote = listOf("A")),
        )
    }

    @Test
    fun concurrentLocalAndRemoteAdditionsBothSurvive() {
        assertEquals(
            listOf("A", "C", "B"),
            mergeCategories(base = listOf("A"), local = listOf("A", "B"), remote = listOf("A", "C")),
        )
    }

    @Test
    fun localRemovalHonoredEvenIfRemoteUnchanged() {
        assertEquals(
            emptyList<String>(),
            mergeCategories(base = listOf("A"), local = listOf(), remote = listOf("A")),
        )
    }

    @Test
    fun localRemovalHonoredWhileRemoteAdds() {
        assertEquals(
            listOf("C"),
            mergeCategories(base = listOf("A"), local = listOf(), remote = listOf("A", "C")),
        )
    }

    @Test
    fun deDupedCaseInsensitivelyPreferringRemoteCasing() {
        assertEquals(
            listOf("Work"),
            mergeCategories(base = listOf(), local = listOf("work"), remote = listOf("Work")),
        )
    }
}
