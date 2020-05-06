package org.tasks.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DescriptionControlSetTest {
    @Test
    fun replaceCRLF() {
        assertEquals("aaa\nbbb", DescriptionControlSet.stripCarriageReturns("aaa\r\nbbb"))
    }

    @Test
    fun replaceCR() {
        assertEquals("aaa\nbbb", DescriptionControlSet.stripCarriageReturns("aaa\rbbb"))
    }

    @Test
    fun dontReplaceLF() {
        assertEquals("aaa\nbbb", DescriptionControlSet.stripCarriageReturns("aaa\nbbb"))
    }

    @Test
    fun checkIfNull() {
        assertNull(DescriptionControlSet.stripCarriageReturns(null))
    }
}