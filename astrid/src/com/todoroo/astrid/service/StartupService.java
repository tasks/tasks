package com.todoroo.astrid.service;

import java.io.File;
import java.util.List;

import org.weloveastrid.rmilk.MilkUtilities;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.util.Log;
import android.widget.Toast;

import com.crittercism.app.Crittercism;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.actfm.sync.ActFmSyncService;
import com.todoroo.astrid.activity.BeastModePreferenceActivity;
import com.todoroo.astrid.backup.BackupConstants;
import com.todoroo.astrid.backup.BackupService;
import com.todoroo.astrid.backup.TasksXmlImporter;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.sync.GtasksSyncOnSaveService;
import com.todoroo.astrid.opencrx.OpencrxCoreUtils;
import com.todoroo.astrid.producteev.ProducteevUtilities;
import com.todoroo.astrid.reminders.ReminderStartupReceiver;
import com.todoroo.astrid.service.abtesting.ABChooser;
import com.todoroo.astrid.service.abtesting.ABOptions;
import com.todoroo.astrid.service.abtesting.FeatureFlipper;
import com.todoroo.astrid.utility.AstridPreferences;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.widget.TasksWidget.WidgetUpdateService;

/**
 * Service which handles jobs that need to be run when Astrid starts up.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class StartupService {


    static {
        AstridDependencyInjector.initialize();
    }

    public StartupService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- application startup

    @Autowired ExceptionService exceptionService;

    @Autowired UpgradeService upgradeService;

    @Autowired TaskService taskService;

    @Autowired MetadataService metadataService;

    @Autowired Database database;

    @Autowired ActFmSyncService actFmSyncService;

    @Autowired GtasksPreferenceService gtasksPreferenceService;

    @Autowired ActFmPreferenceService actFmPreferenceService;

    @Autowired GtasksSyncOnSaveService gtasksSyncOnSaveService;

    @Autowired FeatureFlipper featureFlipper;

    @Autowired ABChooser abChooser;

    /**
     * bit to prevent multiple initializations
     */
    private static boolean hasStartedUp = false;

    /**
     * Call to skip initialization steps (i.e. if only a notification screen is needed)
     */
    public synchronized static void bypassInitialization() {
        hasStartedUp = true;
    }

    /** Called when this application is started up */
    public synchronized void onStartupApplication(final Context context) {
        if(hasStartedUp)
            return;

        // sets up context manager
        ContextManager.setContext(context);

        Crittercism.init(context.getApplicationContext(), Constants.CRITTERCISM_APP_ID,
                Constants.CRITTERCISM_OATH_KEY, Constants.CRITTERCISM_SECRET, StatisticsService.dontCollectStatistics());

        // show notification if reminders are silenced
        if(context instanceof Activity) {
            AudioManager audioManager = (AudioManager)context.getSystemService(
                Context.AUDIO_SERVICE);
            if(!Preferences.getBoolean(R.string.p_rmd_enabled, true))
                Toast.makeText(context, R.string.TLA_notification_disabled, Toast.LENGTH_LONG).show();
            else if(audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0)
                Toast.makeText(context, R.string.TLA_notification_volume_low, Toast.LENGTH_LONG).show();
        }

        // read current version
        int latestSetVersion = 0;
        try {
            latestSetVersion = AstridPreferences.getCurrentVersion();
        } catch (Exception e) {
            exceptionService.reportError("astrid-startup-version-read", e); //$NON-NLS-1$
        }

        if (latestSetVersion == 0) {
            if (Preferences.getLong(AstridPreferences.P_FIRST_LAUNCH, -1) < 0) {
                Preferences.setLong(AstridPreferences.P_FIRST_LAUNCH, DateUtilities.now());
            }

            int defaultTheme = abChooser.getChoiceForOption(ABOptions.AB_THEME_KEY);
            if (defaultTheme == 0)
                Preferences.setString(R.string.p_theme, "white"); //$NON-NLS-1$
            else
                Preferences.setString(R.string.p_theme, "black"); //$NON-NLS-1$
        } else {
            abChooser.setChoiceForOption(ABOptions.AB_THEME_KEY, 0);
            Preferences.setLong(AstridPreferences.P_FIRST_LAUNCH, 0);
        }
        BeastModePreferenceActivity.migrateBeastModePreferences(context);

        int version = 0;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(Constants.PACKAGE, PackageManager.GET_META_DATA);
            version = pi.versionCode;
        } catch (Exception e) {
            exceptionService.reportError("astrid-startup-package-read", e); //$NON-NLS-1$
        }

        Log.i("astrid", "Astrid Startup. " + latestSetVersion + //$NON-NLS-1$ //$NON-NLS-2$
                " => " + version); //$NON-NLS-1$

        databaseRestoreIfEmpty(context);

        // invoke upgrade service
        boolean justUpgraded = latestSetVersion != version;
        if(justUpgraded && version > 0) {
            if(latestSetVersion > 0) {
                upgradeService.performUpgrade(context, latestSetVersion);
            }
            AstridPreferences.setCurrentVersion(version);
        }

        upgradeService.performSecondaryUpgrade(context);

        // perform startup activities in a background thread
        final int finalLatestVersion = latestSetVersion;
        new Thread(new Runnable() {
            public void run() {
                // start widget updating alarm
                AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(context, WidgetUpdateService.class);
                PendingIntent pendingIntent = PendingIntent.getService(context,
                        0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                am.setInexactRepeating(AlarmManager.RTC, 0,
                        Constants.WIDGET_UPDATE_INTERVAL, pendingIntent);

                database.openForWriting();
                taskService.cleanup();

                // if sync ongoing flag was set, clear it
                ProducteevUtilities.INSTANCE.stopOngoing();
                MilkUtilities.INSTANCE.stopOngoing();
                gtasksPreferenceService.stopOngoing();
                actFmPreferenceService.stopOngoing();
                OpencrxCoreUtils.INSTANCE.stopOngoing();

                // perform initialization
                ReminderStartupReceiver.startReminderSchedulingService(context);
                BackupService.scheduleService(context);
                actFmSyncService.initialize();

                gtasksSyncOnSaveService.initialize();

                // get and display update messages
                if (finalLatestVersion != 0)
                    new UpdateMessageService().processUpdates(context);

                // Check for feature flips
                featureFlipper.updateFeatures();
            }
        }).start();

        AstridPreferences.setPreferenceDefaults();

        // check for task killers
        if(!Constants.OEM)
            showTaskKillerHelp(context);

        hasStartedUp = true;
    }

    public static final int INTRO_TASK_SIZE = 0;

    /**
     * If database exists, no tasks but metadata, and a backup file exists, restore it
     */
    private void databaseRestoreIfEmpty(Context context) {
        try {
            if(AstridPreferences.getCurrentVersion() >= UpgradeService.V3_0_0 &&
                    !context.getDatabasePath(database.getName()).exists()) {
                // we didn't have a database! restore latest file
                File directory = BackupConstants.defaultExportDirectory();
                if(!directory.exists())
                    return;
                File[] children = directory.listFiles();
                AndroidUtilities.sortFilesByDateDesc(children);
                if(children.length > 0) {
                    StatisticsService.sessionStart(context);
                    TasksXmlImporter.importTasks(context, children[0].getAbsolutePath(), null);
                    StatisticsService.reportEvent(StatisticsConstants.LOST_TASKS_RESTORED);
                }
            }
        } catch (Exception e) {
            Log.w("astrid-database-restore", e); //$NON-NLS-1$
        }
    }

    private static final String P_TASK_KILLER_HELP = "taskkiller"; //$NON-NLS-1$

    /**
     * Show task killer helper
     * @param context
     */
    private static void showTaskKillerHelp(final Context context) {
        if(!Preferences.getBoolean(P_TASK_KILLER_HELP, false))
            return;

        // search for task killers. if they exist, show the help!
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> apps = pm
                .getInstalledPackages(PackageManager.GET_PERMISSIONS);
        outer: for (PackageInfo app : apps) {
            if(app == null || app.requestedPermissions == null)
                continue;
            if(app.packageName.startsWith("com.android")) //$NON-NLS-1$
                continue;
            for (String permission : app.requestedPermissions) {
                if (Manifest.permission.RESTART_PACKAGES.equals(permission)) {
                    CharSequence appName = app.applicationInfo.loadLabel(pm);
                    OnClickListener listener = new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0,
                                int arg1) {
                            Preferences.setBoolean(P_TASK_KILLER_HELP, true);
                        }
                    };

                    new AlertDialog.Builder(context)
                    .setTitle(R.string.DLG_information_title)
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
