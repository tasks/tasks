/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.test;

import android.content.res.Resources;

import com.todoroo.andlib.test.TranslationTests;

import org.tasks.R;

import java.util.Locale;

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
    public void testSpecialStringsMatch() throws Exception {
        final Resources r = getContext().getResources();
        final StringBuilder failures = new StringBuilder();

        forEachLocale(new Runnable() {
            public void run() {
                contains(r, R.string.CFC_tag_text, failures, "?");
                contains(r, R.string.CFC_title_contains_text, failures, "?");
                contains(r, R.string.CFC_dueBefore_text, failures, "?");
            }
        });

        assertEquals(failures.toString(), 0,
                failures.toString().replaceAll("[^\n]", "").length());
    }
}
