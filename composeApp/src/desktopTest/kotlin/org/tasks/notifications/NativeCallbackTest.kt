package org.tasks.notifications

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NativeCallbackTest {
    @Test
    fun handsBackWhateverTheCallbackAnswered() = runTest {
        val answer = awaiting<String?>(TAG, "posting") { it(null) }

        assertNotNull(answer)
        assertNull(answer!!.value)
    }

    @Test
    fun aCallbackThatNeverComesGivesUp() = runTest {
        assertNull(awaiting<String?>(TAG, "posting", timeoutMs = 10) { })
    }

    @Test
    fun aSecondCallbackIsIgnoredRatherThanResumingTwice() = runTest {
        val answer = awaiting<Int>(TAG, "posting") { answer ->
            answer(1)
            answer(2)
        }

        assertEquals(1, answer?.value)
    }

    companion object {
        private const val TAG = "NativeCallbackTest"
    }
}
