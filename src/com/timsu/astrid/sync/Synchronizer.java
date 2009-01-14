package com.timsu.astrid.sync;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.utilities.DialogUtilities;
import com.timsu.astrid.utilities.Preferences;

public class Synchronizer {

    /* Synchronization Service ID's */
    private static final int SYNC_ID_RTM = 1;

    /** Service map */
    private static Map<Integer, SynchronizationService> services =
        new HashMap<Integer, SynchronizationService>();
    static {
        services.put(SYNC_ID_RTM, new RTMSyncService(SYNC_ID_RTM));
    }

    // --- public interface

    /** Synchronize all activated sync services */
    public static void synchronize(Activity activity) {
        // RTM sync
        if(Preferences.shouldSyncRTM(activity)) {
            services.get(SYNC_ID_RTM).synchronize(activity);
        }
    }

    /** Clears tokens if services are disabled */
    public static void synchronizerStatusUpdated(Activity activity) {
        if(!Preferences.shouldSyncRTM(activity)) {
            services.get(SYNC_ID_RTM).synchronizationDisabled(activity);
        }
    }

    // --- package utilities

    /** Utility class for showing synchronization errors */
    static void showError(Context context, Throwable e) {
        Log.e("astrid", "Synchronization Error", e);

        Resources r = context.getResources();
        DialogUtilities.okDialog(context,
                r.getString(R.string.sync_error) + " " +
                e.getLocalizedMessage(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do nothing?
            }
        });
    }

}
