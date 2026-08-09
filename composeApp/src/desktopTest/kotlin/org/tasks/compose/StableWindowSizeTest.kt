package org.tasks.compose

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StableWindowSizeTest {

    private val collapsed = IntSize(-16, -39)
    private val collapsedDp = DpSize((-16).dp, (-39).dp)

    @Test
    fun reportsTheWindowSizeWhileTheWindowHasOne() {
        val stable = LastValidWindowSize()

        assertEquals(
            IntSize(1280, 800) to DpSize(1280.dp, 800.dp),
            stable.stabilize(IntSize(1280, 800), DpSize(1280.dp, 800.dp)),
        )
    }

    @Test
    fun holdsTheLastSizeWhenTheWindowStopsHavingOne() {
        val stable = LastValidWindowSize()
        stable.stabilize(IntSize(1280, 800), DpSize(1280.dp, 800.dp))

        assertEquals(
            IntSize(1280, 800) to DpSize(1280.dp, 800.dp),
            stable.stabilize(collapsed, collapsedDp),
        )
    }

    @Test
    fun releasesTheHeldSizeWhenTheWindowComesBack() {
        val stable = LastValidWindowSize()
        stable.stabilize(IntSize(1280, 800), DpSize(1280.dp, 800.dp))
        stable.stabilize(collapsed, collapsedDp)

        assertEquals(
            IntSize(640, 480) to DpSize(640.dp, 480.dp),
            stable.stabilize(IntSize(640, 480), DpSize(640.dp, 480.dp)),
        )
    }

    @Test
    fun reportsZeroWhenTheWindowHasNeverHadASize() {
        val stable = LastValidWindowSize()

        assertEquals(IntSize.Zero to DpSize.Zero, stable.stabilize(collapsed, collapsedDp))
    }

    @Test
    fun neverReportsANegativeSize() {
        val beforeAnyLayout = LastValidWindowSize().stabilize(collapsed, collapsedDp)
        val afterLayout = LastValidWindowSize()
            .apply { stabilize(IntSize(1280, 800), DpSize(1280.dp, 800.dp)) }
            .stabilize(collapsed, collapsedDp)

        listOf(beforeAnyLayout, afterLayout).forEach { (px, dp) ->
            assertTrue("$px is negative", px.width >= 0 && px.height >= 0)
            assertTrue("$dp is negative", dp.width >= 0.dp && dp.height >= 0.dp)
        }
    }
}
