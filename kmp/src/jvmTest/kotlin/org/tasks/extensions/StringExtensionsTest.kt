package org.tasks.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

class StringExtensionsTest {
    @Test
    fun aStringInsideTheLimitIsUntouched() {
        assertEquals("Water the plants", "Water the plants".truncate(50))
        assertEquals("Water", "Water".truncate(5))
    }

    @Test
    fun aLongerStringIsCut() {
        assertEquals("Water", "Water the plants".truncate(5))
    }

    @Test
    fun theEllipsisOnlyAppearsWhenSomethingWasActuallyCut() {
        assertEquals("Water…", "Water the plants".truncate(5, "…"))
        assertEquals("Water", "Water".truncate(5, "…"))
    }

    @Test
    fun aCutIsNeverMadeBetweenTheHalvesOfASurrogatePair() {
        val value = "ab🌱"

        assertEquals("ab", value.truncate(3))

        assertEquals(value, value.truncate(4))
        assertEquals("ab", value.truncate(2))
    }

    @Test
    fun aCutThatBacksOffStillSaysItCut() {
        assertEquals("ab…", "ab🌱".truncate(3, "…"))
    }

    @Test
    fun aCapOfZeroOrLessCutsEverything() {
        assertEquals("", "Water the plants".truncate(0))
        assertEquals("", "Water the plants".truncate(-1))

        assertEquals("", "Water the plants".truncate(0, "…"))
        assertEquals("", "".truncate(0))
    }

    @Test
    fun htmlEscapeCoversTheFiveThatMatter() {
        assertEquals(
            "&amp;&lt;&gt;&quot;&#x27;",
            "&<>\"'".htmlEscape(),
        )
    }
}
