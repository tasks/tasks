package com.timsu.astrid.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.activities.SyncPreferences;
import com.timsu.astrid.appwidget.AstridAppWidgetProvider.UpdateService;
import com.timsu.astrid.sync.SynchronizationService;

public class StartupReceiver extends BroadcastReceiver {

    private static boolean hasStartedUp = false;

    @Override
    /** Called when the system is started up */
    public void onReceive(Context context, Intent intent) {
        Notifications.scheduleAllAlarms(context);
    }

    /** Called when this application is started up */
    public static void onStartupApplication(final Context context) {
        if(hasStartedUp)
            return;

        int latestSetVersion = Preferences.getCurrentVersion(context);
        int version = 0;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo("com.timsu.astrid", 0);
            version = pi.versionCode;
        } catch (Exception e) {
            Log.e("StartupAstrid", "Error getting version!", e);
        }

        // if we just got upgraded, set the alarms
        boolean justUpgraded = latestSetVersion != version;
        final int finalVersion = version;
        if(justUpgraded) {
        	// perform version-specific processing
        	if(latestSetVersion <= 99) {
        		if(Preferences.getSyncOldAutoSyncFrequency(context) != null) {
        			float value = Preferences.getSyncOldAutoSyncFrequency(context);
        			Preferences.setSyncAutoSyncFrequency(context,
        					Math.round(value * 3600));
        			DialogUtilities.okDialog(context, context.getResources().getString(
        			        R.string.sync_upgrade_v99), new OnClickListener() {
        			    public void onClick(DialogInterface dialog,
        			            int which) {
        			        context.startActivity(new Intent(context, SyncPreferences.class));
        			    }
        			});
        		}
        	}

        	Preferences.setCurrentVersion(context, finalVersion);
        }


        // schedule alarms in background every time Astrid is run
        new Thread(new Runnable() {
            public void run() {
                Notifications.scheduleAllAlarms(context);
            }
        }).start();

        Preferences.setPreferenceDefaults(context);

        // start synchronization service
        SynchronizationService.scheduleService(context);

        // update widget
        Intent intent = new Intent(context, UpdateService.class);
        context.startService(intent);

        hasStartedUp = true;
    }
}
