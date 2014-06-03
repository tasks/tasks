/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.service;

import android.app.Activity;
import android.content.Context;

/**
 * Singleton class to manage current application context
 * b
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Deprecated
public final class ContextManager {

    /**
     * Global application context
     */
    private static Context context = null;

    /**
     * Sets the global context
     */
    public static void setContext(Context context) {
        if(context == null || context.getApplicationContext() == null) {
            return;
        }
        if(ContextManager.context != null && !(context instanceof Activity)) {
            return;
        }
        ContextManager.context = context;
    }

    /**
     * Gets the global context
     */
    public static Context getContext() {
        return context;
    }
}
