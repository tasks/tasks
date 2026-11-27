package org.tasks.mcp

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.BindException

class StartFailureMessageTest {
    @Test
    fun reportsTheCauseRatherThanTheCoroutineWrapper() {
        val failure = IllegalStateException(
            "LazyStandaloneCoroutine is cancelling; job=LazyStandaloneCoroutine{Cancelling}@6520d866",
            BindException("Address already in use"),
        )

        assertEquals("Address already in use", failure.describe())
    }

    @Test
    fun reachesPastEveryWrapper() {
        val failure = RuntimeException(
            "outer",
            IllegalStateException("middle", BindException("Address already in use")),
        )

        assertEquals("Address already in use", failure.describe())
    }

    @Test
    fun fallsBackToTheClassNameWhenTheCauseCarriesNoMessage() {
        val failure = IllegalStateException("cancelling", BindException())

        assertEquals("BindException", failure.describe())
    }

    @Test
    fun stopsOnACauseThatPointsBack() {
        val first = IllegalStateException("first")
        val second = IllegalStateException("second", first)
        first.initCause(second)

        assertEquals("second", first.describe())
    }
}
