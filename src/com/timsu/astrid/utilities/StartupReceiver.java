package com.timsu.astrid.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {

    private static boolean hasStartedUp = false;

    @Override
    /** Called when the system is started up */
    public void onReceive(Context context, Intent intent) {
        Notifications.scheduleAllAlarms(context);
    }

    /** Called when this application is started up */
    public static void onStartupApplication(Context context) {
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
        if(justUpgraded) {
            Notifications.scheduleAllAlarms(context);
            Preferences.setCurrentVersion(context, version);
        }

        Preferences.setPreferenceDefaults(context);

        hasStartedUp = true;
    }
}
