package org.tasks.compose.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The merge is the forward/backward-compatibility contract of the "Customize
 * edit screen" persistence: a stored arrangement from any build must survive
 * a build change that adds or removes sections without migration.
 */
class EditScreenSectionsMergeTest {

    @Test
    fun emptyStoredYieldsCanonicalAllEnabled() {
        val items = EditScreenSections.merge(emptyList())
        assertEquals(EditScreenSections.CANONICAL, items.map { it.section })
        assertTrue(items.all { it.enabled })
    }

    @Test
    fun storedOrderAndEnabledFlagsAreRespected() {
        val stored = listOf(
            EditScreenSectionPref(EditScreenSection.TAGS.name, enabled = false),
            EditScreenSectionPref(EditScreenSection.LIST.name),
        )
        val items = EditScreenSections.merge(stored)
        assertEquals(9, items.size)
        assertEquals(EditScreenSection.TAGS, items[0].section)
        assertEquals(false, items[0].enabled)
        assertEquals(EditScreenSection.LIST, items[1].section)
        assertEquals(true, items[1].enabled)
        // Untouched sections follow in canonical order, enabled.
        val rest = items.drop(2).map { it.section }
        val canonicalRest = EditScreenSections.CANONICAL.filter { it != EditScreenSection.TAGS && it != EditScreenSection.LIST }
        assertEquals(canonicalRest, rest)
        assertTrue(items.drop(2).all { it.enabled })
    }

    @Test
    fun unknownIdsAreDropped() {
        val items = EditScreenSections.merge(
            listOf(EditScreenSectionPref("NO_SUCH_SECTION", enabled = false)),
        )
        assertEquals(EditScreenSections.CANONICAL, items.map { it.section })
        assertTrue(items.all { it.enabled })
    }

    @Test
    fun sectionsAddedSinceStorageAreAppendedEnabled() {
        // Simulate an older build that stored every section except the
        // newest one (SUBTASKS here stands in for any future addition).
        val stored = EditScreenSection.entries.dropLast(1).map { EditScreenSectionPref(it.name) }
        val items = EditScreenSections.merge(stored)
        assertEquals(9, items.size)
        assertEquals(EditScreenSection.SUBTASKS, items.last().section)
        assertTrue(items.last().enabled)
    }
}
