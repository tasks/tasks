/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.example.astrid.filter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This class identifies the add-on to Astrid so users can re-order their
 * add-ons or toggle their visibility.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterAddon extends BroadcastReceiver {

    /**
     * Allows your plugin intents to identify themselves
     */
    static final String IDENTIFIER = "samplefilter"; //$NON-NLS-1$

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO coming in v3.0.0
    }

}
