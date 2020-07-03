package org.tasks.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.ui.TaskEditViewModel.Companion.stripCarriageReturns

class TaskEditViewModelTest {
    @Test
    fun replaceCRLF() {
        assertEquals("aaa\nbbb", "aaa\r\nbbb".stripCarriageReturns())
    }

    @Test
    fun replaceCR() {
        assertEquals("aaa\nbbb", "aaa\rbbb".stripCarriageReturns())
    }

    @Test
    fun dontReplaceLF() {
        assertEquals("aaa\nbbb", "aaa\nbbb".stripCarriageReturns())
    }

    @Test
    fun stripCarriageReturnOnNull() {
        assertNull((null as String?).stripCarriageReturns())
    }
}