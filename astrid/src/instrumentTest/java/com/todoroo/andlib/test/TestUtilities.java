/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.test;


/**
 * Utility methods used in unit tests
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TestUtilities {

    /**
     * Sleep, suppressing exceptions
     *
     * @param millis
     */
    public static void sleepDeep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

}
