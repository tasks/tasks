package org.tasks.googleapis

import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleTaskTruncationTest {
    @Test
    fun aTitleCutShortForASurrogatePairDoesNotOverwriteTheLocalOne() {
        val local = "abcdef🌱ghij"

        val pushed = GoogleTaskSynchronizer.truncate(local, 7)
        assertEquals("abcdef", pushed)

        assertEquals(local, GoogleTaskSynchronizer.getTruncatedValue(local, pushed, 7))
    }

    @Test
    fun aTitleCutAtTheCapDoesNotOverwriteTheLocalOne() {
        val local = "abcdefghij"

        val pushed = GoogleTaskSynchronizer.truncate(local, 7)
        assertEquals("abcdefg", pushed)

        assertEquals(local, GoogleTaskSynchronizer.getTruncatedValue(local, pushed, 7))
    }

    @Test
    fun aValueShorterThanAnyTruncationStillReplacesTheLocalOne() {
        assertEquals("abc", GoogleTaskSynchronizer.getTruncatedValue("abcdefghij", "abc", 7))
    }

    @Test
    fun anEditThatLandsOnTheBackedOffLengthIsKept() {
        val local = "abcdefghij"

        assertEquals("abcdefg", GoogleTaskSynchronizer.truncate(local, 7))

        assertEquals("abcdef", GoogleTaskSynchronizer.getTruncatedValue(local, "abcdef", 7))
    }

    @Test
    fun aValueThatIsNotAPrefixReplacesTheLocalOneAtEitherLength() {
        assertEquals("zzzzzz", GoogleTaskSynchronizer.getTruncatedValue("abcdefghij", "zzzzzz", 7))
        assertEquals("zzzzzzz", GoogleTaskSynchronizer.getTruncatedValue("abcdefghij", "zzzzzzz", 7))
    }
}
