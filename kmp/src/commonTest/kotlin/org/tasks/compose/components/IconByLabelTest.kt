package org.tasks.compose.components

import org.tasks.icons.MaterialSymbols
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IconByLabelTest {
    @Test
    fun materialIconsNamesResolve() {
        assertNotNull(MaterialSymbols.codepoint("home"))
        assertNotNull(MaterialSymbols.codepoint("all_inbox"))
        assertNotNull(MaterialSymbols.codepoint("10k"))
        assertEquals(0xFFF8B, MaterialSymbols.codepoint("code_xml"))
        assertEquals("\uDBBF\uDF8B", MaterialSymbols.glyph("code_xml"))
        assertEquals("\uE9B2", MaterialSymbols.glyph("home"))
        assertEquals(MaterialSymbols.codepoint("mail"), MaterialSymbols.codepoint("email"))
        assertEquals(MaterialSymbols.codepoint("play_circle"), MaterialSymbols.codepoint("play_circle_filled"))
    }

    @Test
    fun theLegacyGmoPrefixIsStripped() {
        assertEquals("beach_access", "gmo_beach_access".iconName)
        assertEquals("beach_access", "beach_access".iconName)
        assertTrue(iconExists("gmo_beach_access"))
    }

    @Test
    fun unknownNamesResolveToNothing() {
        assertNull(MaterialSymbols.codepoint("definitely_not_an_icon"))
        assertNull(MaterialSymbols.codepoint(""))
        assertFalse(iconExists(""))
        assertFalse(iconExists(null))
    }
}
