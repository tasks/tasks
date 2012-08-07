/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.widget;


import android.app.Activity;

import com.todoroo.andlib.utility.AndroidUtilities;




/**
 * All API versions-friendly gesture detector. On SDK < 4, nothing happens
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GestureService {

    public interface GestureInterface {
        public void gesturePerformed(String gesture);
    }

    /**
     * Register gesture detector. If  android SDK version is not correct,
     * a {@link VerifyError} will be throw. Catch this explicitly.
     *
     * @param activity
     * @param view
     * @param gestureLibrary
     * @param listener
     * @throws VerifyError
     */
    public static void registerGestureDetector(Activity activity, int view,
            int gestureLibrary, GestureInterface listener) throws VerifyError {
        if(AndroidUtilities.getSdkVersion() > 3)
            new Api4GestureDetector(activity, view, gestureLibrary, listener);
    }

}
