/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.test;



import java.util.Locale;

import android.content.res.Resources;

import com.timsu.astrid.R;
import com.todoroo.andlib.test.TranslationTests;

public class AstridTranslationTests extends TranslationTests {

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
     * @param string
     * @param contains
     * @return
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

                contains(r, R.string.WID_dateButtonLabel, failures, "$D", "$T");
                contains(r, R.string.locale_notification, failures, "$NUM", "$FILTER");
                contains(r, R.string.repeat_detail_byday, failures, "$I", "$D");
            }
        });

        assertEquals(failures.toString(), 0,
                failures.toString().replaceAll("[^\n]", "").length());
    }

}
