package org.tasks.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RetryAfterTest {
    @Test
    fun readsAWaitInSeconds() {
        assertEquals(20_000L, "20".retryAfterMillis())
    }

    @Test
    fun readsAWaitGivenAsADate() {
        assertEquals(60_000L, "Thu, 01 Jan 1970 00:01:00 GMT".retryAfterMillis(now = 0L))
    }

    @Test
    fun aDateThatHasPassedIsNoAdviceAtAll() {
        assertNull("Thu, 01 Jan 1970 00:01:00 GMT".retryAfterMillis(now = 120_000L))
    }

    @Test
    fun comeBackNowIsNoAdviceEither() {
        assertNull("0".retryAfterMillis())
    }

    @Test
    fun ignoresAHeaderItCannotRead() {
        assertNull("soon".retryAfterMillis())
        assertNull("".retryAfterMillis())
    }
}
