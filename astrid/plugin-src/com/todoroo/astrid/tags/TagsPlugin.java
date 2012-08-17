/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.actfm.TagSettingsActivity;
import com.todoroo.astrid.actfm.TagSettingsActivityTablet;
import com.todoroo.astrid.api.Addon;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.utility.AstridPreferences;

public class TagsPlugin extends BroadcastReceiver {

    static final String IDENTIFIER = "tags"; //$NON-NLS-1$

    @SuppressWarnings("nls")
    @Override
    public void onReceive(Context context, Intent intent) {
        Addon plugin = new Addon(IDENTIFIER, "Tags", "Todoroo",
                "Provides tagging support for tasks.");

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ADDONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, plugin);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }


    /**
     * Create new tag data
     * @param activity
     */
    public static Intent newTagDialog(Context context) {
        Class<?> settingsComponent = AstridPreferences.useTabletLayout(context) ? TagSettingsActivityTablet.class : TagSettingsActivity.class;
        Intent intent = new Intent(context, settingsComponent);
        return intent;
    }

}
