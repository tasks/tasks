/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.test

import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.tasks.R.string
import java.util.*

/**
 * Tests translations for consistency with the default values. You must extend this class and create
 * it with your own values for strings and arrays.
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
@RunWith(AndroidJUnit4::class)
class TranslationTests {
    /** Loop through each locale and call runnable  */
    private fun forEachLocale(callback: (Resources) -> Unit) {
        val locales = Locale.getAvailableLocales()
        for (locale in locales) {
            callback.invoke(getResourcesForLocale(locale))
        }
    }

    private fun getResourcesForLocale(locale: Locale): Resources {
        val resources = InstrumentationRegistry.getTargetContext().resources
        val configuration = Configuration(resources.configuration)
        configuration.locale = locale
        return Resources(resources.assets, resources.displayMetrics, configuration)
    }

    /** Internal test of format string parser  */
    @Test
    fun testFormatStringParser() {
        var s = "abc"
        var data = FormatStringData(s)
        assertEquals(s, data.string)
        assertEquals(0, data.characters.size)
        s = "abc %s def"
        data = FormatStringData(s)
        assertEquals(1, data.characters.size)
        assertEquals('s', data.characters[0])
        s = "abc %%s def %d"
        data = FormatStringData(s)
        assertEquals(2, data.characters.size)
        assertEquals('%', data.characters[0])
        assertEquals('d', data.characters[1])
        assertTrue(data.toString(), data.toString().contains("[%"))
        assertTrue(data.toString(), data.toString().contains("d]"))
        assertTrue(data.toString(), data.toString().contains(s))
        assertTrue(data.matches(FormatStringData("espanol %% und %d si")))
        assertFalse(data.matches(FormatStringData("ingles %d ja %% pon")))
        s = "% abc %"
        data = FormatStringData(s)
        assertEquals(2, data.characters.size)
        assertEquals(' ', data.characters[0])
        assertEquals('\u0000', data.characters[1])
    }

    /**
     * Test that the format specifiers in translations match exactly the translations in the default
     * text
     */
    @Test
    fun testFormatStringsMatch() {
        val resources = InstrumentationRegistry.getTargetContext().resources
        val strings = getResourceIds(string::class.java)
        val formatStrings = arrayOfNulls<FormatStringData>(strings.size)
        val failures = StringBuilder()
        for (i in strings.indices) {
            try {
                val string = resources.getString(strings[i])
                formatStrings[i] = FormatStringData(string)
            } catch (e: Exception) {
                val name = resources.getResourceName(strings[i])
                failures.append(String.format("error opening %s: %s\n", name, e.message))
            }
        }
        forEachLocale { r: Resources ->
            val locale = r.configuration.locale
            for (i in strings.indices) {
                try {
                    if (strings[i] == string.abc_shareactionprovider_share_with_application) {
                        continue
                    }
                    val string = r.getString(strings[i])
                    val newFS = FormatStringData(string)
                    if (!newFS.matches(formatStrings[i])) {
                        val name = r.getResourceName(strings[i])
                        failures.append(String.format(
                                "%s (%s): %s != %s\n", name, locale.toString(), newFS, formatStrings[i]))
                    }
                } catch (e: Exception) {
                    val name = r.getResourceName(strings[i])
                    failures.append(String.format(
                            "%s: error opening %s: %s\n", locale.toString(), name, e.message))
                }
            }
        }
        assertEquals(failures.toString(), 0, errorCount(failures))
    }

    /** check if string contains contains substrings  */
    private fun contains(r: Resources, resource: Int, failures: StringBuilder, expected: String) {
        val translation = r.getString(resource)
        if (!translation.contains(expected)) {
            val locale = r.configuration.locale
            val name = r.getResourceName(resource)
            failures.append(String.format("%s: %s did not contain: %s\n", locale.toString(), name, expected))
        }
    }

    /** Test dollar sign resources  */
    @Test
    fun testSpecialStringsMatch() {
        val failures = StringBuilder()
        forEachLocale { r: Resources ->
            contains(r, string.CFC_tag_text, failures, "?")
            contains(r, string.CFC_title_contains_text, failures, "?")
            contains(r, string.CFC_dueBefore_text, failures, "?")
            contains(r, string.CFC_tag_contains_text, failures, "?")
            contains(r, string.CFC_gtasks_list_text, failures, "?")
        }
        assertEquals(failures.toString(), 0, failures.toString().replace("[^\n]".toRegex(), "").length)
    }

    /** Count newlines  */
    private fun errorCount(failures: StringBuilder): Int {
        var count = 0
        var pos = -1
        while (true) {
            pos = failures.indexOf("\n", pos + 1)
            if (pos == -1) {
                return count
            }
            count++
        }
    }

    /** @return an array of all string resource id's
     */
    private fun getResourceIds(resources: Class<*>): IntArray {
        val fields = resources.declaredFields
        val ids: MutableList<Int> = ArrayList(fields.size)
        for (field in fields) {
            try {
                ids.add(field.getInt(null))
            } catch (e: Exception) {
                // not a field we care about
            }
        }
        val idsAsIntArray = IntArray(ids.size)
        for (i in ids.indices) {
            idsAsIntArray[i] = ids[i]
        }
        return idsAsIntArray
    }

    private class FormatStringData constructor(
            /** the original string  */
            val string: String) {
        /** format characters  */
        val characters: CharArray

        /** test that the characters match  */
        fun matches(other: FormatStringData?): Boolean {
            if (characters.size != other!!.characters.size) {
                return false
            }
            outer@ for (i in characters.indices) {
                if (Character.isDigit(characters[i])) {
                    for (j in other.characters.indices) {
                        if (characters[i] == other.characters[j]) {
                            break@outer
                        }
                    }
                    return false
                } else if (characters[i] != other.characters[i]) {
                    return false
                }
            }
            return true
        }

        override fun toString(): String {
            val value = StringBuilder("[")
            for (i in characters.indices) {
                value.append(characters[i])
                if (i < characters.size - 1) {
                    value.append(',')
                }
            }
            value.append("]: '").append(string).append('\'')
            return value.toString()
        }

        companion object {
            private val scratch = CharArray(10)
        }

        init {
            var pos = -1
            var count = 0
            while (true) {
                pos = string.indexOf('%', ++pos)
                if (pos++ == -1) {
                    break
                }
                if (pos >= string.length) {
                    scratch[count++] = '\u0000'
                } else {
                    scratch[count++] = string[pos]
                }
            }
            characters = CharArray(count)
            for (i in 0 until count) {
                characters[i] = scratch[i]
            }
        }
    }
}