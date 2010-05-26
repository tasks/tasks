package com.timsu.astrid.utilities;

import java.util.List;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
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
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.UpgradeService;

public class StartupReceiver extends BroadcastReceiver {

    private static boolean hasStartedUp = false;

    static {
        AstridDependencyInjector.initialize();
    }

    @Override
    /** Called when the system is started up */
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        Notifications.scheduleAllAlarms(context);
    }

    /** Called when this application is started up */
    public static void onStartupApplication(final Context context) {
        if(hasStartedUp)
            return;

        ContextManager.setContext(context);

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
        	new UpgradeService().performUpgrade(latestSetVersion);
        }


        // perform startup activities in a background thread
        new Thread(new Runnable() {
            public void run() {
                // schedule alarms
                Notifications.scheduleAllAlarms(context);

                // start widget updating alarm
                AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(context, UpdateService.class);
                PendingIntent pendingIntent = PendingIntent.getService(context,
                        0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                am.setInexactRepeating(AlarmManager.RTC, 0,
                        Constants.WIDGET_UPDATE_INTERVAL, pendingIntent);

                // start synchronization service
                if(Constants.SYNCHRONIZE)
                    SynchronizationService.scheduleService(context);

                // start backup service
                BackupService.scheduleService(context);
            }
        }).start();

        Preferences.setPreferenceDefaults(context);

        // check for task killers
        if(!Constants.OEM)
            showTaskKillerHelp(context);

        hasStartedUp = true;
    }

    private static void showTaskKillerHelp(final Context context) {
        if(!Preferences.shouldShowTaskKillerHelp(context))
            return;

        // search for task killers. if they exist, show the help!
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> apps = pm
                .getInstalledPackages(PackageManager.GET_PERMISSIONS);
        outer: for (PackageInfo app : apps) {
            if(app == null || app.requestedPermissions == null)
                continue;
            if(app.packageName.startsWith("com.android"))
                continue;
            for (String permission : app.requestedPermissions) {
                if (Manifest.permission.RESTART_PACKAGES.equals(permission)) {
                    CharSequence appName = app.applicationInfo.loadLabel(pm);
                    Log.e("astrid", "found task killer: " + app.packageName);

                    OnClickListener listener = new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0,
                                int arg1) {
                            Preferences.disableTaskKillerHelp(context);
                        }
                    };

                    new AlertDialog.Builder(context)
                    .setTitle(R.string.information_title)
                    .setMessage(context.getString(R.string.task_killer_help,
                            appName))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.task_killer_help_ok, listener)
                    .show();

                    break outer;
                }
            }
        }
    }
}
