/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.service;

import android.content.Context;
import android.content.res.Resources;

/**
 * Singleton class to manage current application context
 * b
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class ContextManager {

    /**
     * Global application context
     */
    private static Context context = null;

    /**
     * Sets the global context
     * @param context
     */
    public static void setContext(Context context) {
        ContextManager.context = context;
    }

    /**
     * Gets the global context
     */
    public static Context getContext() {
        return context;
    }

    /**
     * Convenience method to read a string from the resources
     *
     * @param resid
     * @param parameters
     * @return
     */
    public static String getString(int resId, Object... formatArgs) {
        return context.getString(resId, formatArgs);
    }

    /**
     * Convenience method to read resources
     *
     * @return
     */
    public static Resources getResources() {
        return context.getResources();
    }

}
