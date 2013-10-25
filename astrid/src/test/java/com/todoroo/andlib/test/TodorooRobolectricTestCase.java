/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.test;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.service.AstridDependencyInjector;

import org.junit.After;
import org.junit.Before;
import org.robolectric.Robolectric;

import java.util.Locale;

public class TodorooRobolectricTestCase {

    static {
        AstridDependencyInjector.initialize();
    }

    @Before
    public void setUp() throws Exception {
        ContextManager.setContext(getContext());
        AstridDependencyInjector.flush();
        DependencyInjectionService.getInstance().inject(this);
        setLocale(Locale.ENGLISH);
    }

    @After
    public void tearDown() throws Exception {
        setLocale(Locale.getDefault());
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

    protected Context getContext() {
        return Robolectric.getShadowApplication().getApplicationContext();
    }
}
