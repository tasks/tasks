/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.test;

import android.content.res.Resources;
import android.support.test.runner.AndroidJUnit4;

import com.todoroo.andlib.test.TranslationTests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.tasks.R;

import java.util.Locale;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AstridTranslationTest extends TranslationTests {

    @Override
    public Class<?> getArrayResources() {
        return R.array.class;
    }

    @Override
    public Class<?> getStringResources() {
        return R.string.class;
    }

    @Override
    public int[] getDateFormatStrings() {
        return new int[] {
                //
        };
    }

    /**
     * check if string contains contains substrings
     */
    public void contains(Resources r, int resource, StringBuilder failures, String... contains) {
        String string = r.getString(resource);
        for(String contain : contains)
            if(!string.contains(contain)) {
                Locale locale = r.getConfiguration().locale;
                String name = r.getResourceName(resource);
                failures.append(String.format("%s: %s did not contain: %s\n",
                        locale.toString(), name, contain));
            }
    }

    /**
     * Test dollar sign resources
     */
    @Test
    public void testSpecialStringsMatch() throws Exception {
        final Resources r = getTargetContext().getResources();
        final StringBuilder failures = new StringBuilder();

        forEachLocale(() -> {
            contains(r, R.string.CFC_tag_text, failures, "?");
            contains(r, R.string.CFC_title_contains_text, failures, "?");
            contains(r, R.string.CFC_dueBefore_text, failures, "?");
        });

        assertEquals(failures.toString(), 0,
                failures.toString().replaceAll("[^\n]", "").length());
    }
}
