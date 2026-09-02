package org.tasks.compose.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.compose.pickers.label

class IconNameTest {
    @Test
    fun aMaterialSymbolsNameResolves() {
        assertNotNull(imageVectorByName("beach_access"))
    }

    @Test
    fun theLegacyGmoPrefixIsStripped() {
        assertEquals("BeachAccess", "gmo_beach_access".label)
        assertNotNull(imageVectorByName("gmo_beach_access"))
        assertEquals(imageVectorByName("beach_access"), imageVectorByName("gmo_beach_access"))
    }

    @Test
    fun aLeadingDigitStillGetsItsUnderscore() {
        assertEquals("_10k", "10k".label)
        assertEquals("_10k", "gmo_10k".label)
    }

    @Test
    fun anEmptyIconNameIsNotAnError() {
        assertEquals("", "".label)
        assertNull(imageVectorByName(""))
    }

    @Test
    fun anUnknownNameResolvesToNothing() {
        assertNull(imageVectorByName("definitely_not_an_icon"))
    }
}
