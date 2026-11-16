package com.todoroo.astrid.service

import com.todoroo.astrid.service.Upgrader.Companion.isMajorOrMinorUpdate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpgraderTest {
    @Test
    fun minorUpdate() = assertTrue(isMajorOrMinorUpdate(151306, 151404)) // 15.13 -> 15.14

    @Test
    fun majorUpdate() = assertTrue(isMajorOrMinorUpdate(141400, 150000)) // 14.14 -> 15.0

    @Test
    fun skipMinorToPatch() = assertTrue(isMajorOrMinorUpdate(151306, 151406)) // 15.13 -> 15.14.1

    @Test
    fun patchUpdate() = assertFalse(isMajorOrMinorUpdate(151404, 151406)) // 15.14 -> 15.14.1

    @Test
    fun betaToRelease() = assertFalse(isMajorOrMinorUpdate(151402, 151404)) // 15.14-beta02 -> 15.14
}
