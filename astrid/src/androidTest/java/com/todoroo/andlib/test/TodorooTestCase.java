/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.test;

import android.content.res.Configuration;
import android.test.AndroidTestCase;
import android.util.DisplayMetrics;

import com.todoroo.andlib.service.ContextManager;

import java.util.Locale;

/**
 * Base test case for Astrid tests
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public abstract class TodorooTestCase extends AndroidTestCase {

	@Override
	protected void setUp() {
        try {
            super.setUp();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }

        ContextManager.setContext(getContext());
        setLocale(Locale.ENGLISH);
	}

    @Override
    protected void tearDown() {
        try {
            super.tearDown();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        setLocale(Locale.getDefault());
    }

    /**
     * Sets locale
     */
    private void setLocale(Locale locale) {
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        getContext().getResources().updateConfiguration(config, metrics);
    }

    /**
     * Loop through each locale and call runnable
     */
    public void forEachLocale(Runnable r) {
        Locale[] locales = Locale.getAvailableLocales();
        for(Locale locale : locales) {
            setLocale(locale);

            r.run();
        }
    }
}
