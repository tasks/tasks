/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.helper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.ArrayAdapter;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.SyncAction;
import com.todoroo.astrid.gtasks.GtasksPreferences;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.sync.SyncV2Provider;

/**
 * SyncActionHelper is a helper class for encapsulating UI actions
 * responsible for performing sync and prompting user to sign up for a new
 * sync service.
 *
 * In order to make this work you need to call register() and unregister() in
 * onResume and onPause, respectively.
 *
 * @author Tim Su <tim@astrid.com>
 *
 */
public class SyncActionHelper {

    public static final String PREF_LAST_AUTO_SYNC = "taskListLastAutoSync"; //$NON-NLS-1$

    private final LinkedHashSet<SyncAction> syncActions = new LinkedHashSet<SyncAction>();

    public final SyncResultCallback syncResultCallback;

    private final Activity activity;

    private final Fragment fragment;

    protected SyncActionReceiver syncActionReceiver = new SyncActionReceiver();

    @Autowired SyncV2Service syncService;
    @Autowired ExceptionService exceptionService;

    // --- boilerplate

    public SyncActionHelper(Activity activity, Fragment fragment) {
        DependencyInjectionService.getInstance().inject(this);

        this.activity = activity;
        this.fragment = fragment;
        syncResultCallback = new ProgressBarSyncResultCallback(activity, fragment,
                R.id.progressBar, new Runnable() {
                    @Override
                    public void run() {
                        ContextManager.getContext().sendBroadcast(
                                new Intent(
                                        AstridApiConstants.BROADCAST_EVENT_REFRESH));
                    }
                });
    }

    // --- automatic sync logic

    public void initiateAutomaticSync() {
        long tasksPushedAt = Preferences.getLong(PREF_LAST_AUTO_SYNC, 0);
        if (DateUtilities.now() - tasksPushedAt > TaskListFragment.AUTOSYNC_INTERVAL) {
            performSyncServiceV2Sync(false);
        }
    }

    // --- sync action receiver logic

    /**
     * Receiver which receives sync provider intents
     *
     */
    protected class SyncActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || !AstridApiConstants.BROADCAST_SEND_SYNC_ACTIONS.equals(intent.getAction()))
                return;

            try {
                Bundle extras = intent.getExtras();
                SyncAction syncAction = extras.getParcelable(AstridApiConstants.EXTRAS_RESPONSE);
                syncActions.add(syncAction);
            } catch (Exception e) {
                exceptionService.reportError("receive-sync-action-" + //$NON-NLS-1$
                        intent.getStringExtra(AstridApiConstants.EXTRAS_ADDON),
                        e);
            }
        }
    }

    public void register() {
        activity.registerReceiver(
                syncActionReceiver,
                new IntentFilter(AstridApiConstants.BROADCAST_SEND_SYNC_ACTIONS));
    }

    public void unregister() {
        AndroidUtilities.tryUnregisterReceiver(activity, syncActionReceiver);
    }

    public void request() {
        syncActions.clear();
        Intent broadcastIntent = new Intent(
                AstridApiConstants.BROADCAST_REQUEST_SYNC_ACTIONS);
        activity.sendOrderedBroadcast(broadcastIntent,
                AstridApiConstants.PERMISSION_READ);
    }

    // --- sync logic

    protected void performSyncServiceV2Sync(boolean manual) {
        boolean syncOccurred = syncService.synchronizeActiveTasks(manual, syncResultCallback);
        if (syncOccurred)
            Preferences.setLong(PREF_LAST_AUTO_SYNC, DateUtilities.now());
    }

    /**
     * Intent object with custom label returned by toString.
     *
     * @author joshuagross <joshua.gross@gmail.com>
     */
    protected static class IntentWithLabel extends Intent {
        private final String label;

        public IntentWithLabel(Intent in, String labelIn) {
            super(in);
            label = labelIn;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public void performSyncAction() {
        List<SyncV2Provider> activeV2Providers = syncService.activeProviders();
        int activeSyncs = syncActions.size() + activeV2Providers.size();

        if (activeSyncs == 0) {
            String desiredCategory = activity.getString(R.string.SyP_label);

            // Get a list of all sync plugins and bring user to the prefs pane
            // for one of them
            Intent queryIntent = new Intent(AstridApiConstants.ACTION_SETTINGS);
            PackageManager pm = activity.getPackageManager();
            List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(
                    queryIntent, PackageManager.GET_META_DATA);
            int length = resolveInfoList.size();
            ArrayList<Intent> syncIntents = new ArrayList<Intent>();

            // Loop through a list of all packages (including plugins, addons)
            // that have a settings action: filter to sync actions
            for (int i = 0; i < length; i++) {
                ResolveInfo resolveInfo = resolveInfoList.get(i);
                Intent intent = new Intent(AstridApiConstants.ACTION_SETTINGS);
                intent.setClassName(resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name);

                String category = MetadataHelper.resolveActivityCategoryName(
                        resolveInfo, pm);

                if (GtasksPreferences.class.getName().equals(
                        resolveInfo.activityInfo.name))
                    continue;

                if (resolveInfo.activityInfo.metaData != null) {
                    Bundle metadata = resolveInfo.activityInfo.metaData;
                    if (!metadata.getBoolean("syncAction")) //$NON-NLS-1$
                        continue;
                }

                if (category.equals(desiredCategory)) {
                    syncIntents.add(new IntentWithLabel(intent,
                            resolveInfo.activityInfo.loadLabel(pm).toString()));
                }
            }

            final Intent[] actions = syncIntents.toArray(new Intent[syncIntents.size()]);
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface click, int which) {
                    fragment.startActivityForResult(actions[which], TaskListFragment.ACTIVITY_SETTINGS);
                }
            };
            if (actions.length == 1) {
                fragment.startActivityForResult(actions[0], TaskListFragment.ACTIVITY_SETTINGS);
            } else {
                showSyncOptionMenu(actions, listener);
            }

        } else {
            syncService.synchronizeActiveTasks(true, syncResultCallback);
        }
    }

    /**
     * Show menu of sync options. This is shown when you're not logged into any
     * services, or logged into more than one.
     *
     * @param <TYPE>
     * @param items
     * @param listener
     */
    private <TYPE> void showSyncOptionMenu(TYPE[] items,
            DialogInterface.OnClickListener listener) {
        if (items.length == 1) {
            listener.onClick(null, 0);
            return;
        }

        ArrayAdapter<TYPE> adapter = new ArrayAdapter<TYPE>(activity,
                android.R.layout.simple_spinner_dropdown_item, items);

        // show a menu of available options
        new AlertDialog.Builder(activity).setTitle(R.string.Sync_now_label).setAdapter(
                adapter, listener).show().setOwnerActivity(activity);
    }

}


