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
import org.junit.Assert.assertEquals
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
            callback(getResourcesForLocale(locale))
        }
    }

    private fun getResourcesForLocale(locale: Locale): Resources {
        val resources = InstrumentationRegistry.getTargetContext().resources
        val configuration = Configuration(resources.configuration)
        configuration.locale = locale
        return Resources(resources.assets, resources.displayMetrics, configuration)
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
}