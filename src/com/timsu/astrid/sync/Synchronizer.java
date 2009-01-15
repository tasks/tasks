package com.timsu.astrid.sync;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;

import com.timsu.astrid.data.sync.SyncDataController;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.utilities.Preferences;

public class Synchronizer {

    // Synchronization Service ID's
    private static final int SYNC_ID_RTM = 1;

    // --- public interface

    /** Synchronize all activated sync services */
    public static void synchronize(final Activity activity) {
        // kick off a new thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                // RTM sync
                if(Preferences.shouldSyncRTM(activity)) {
                    openControllers(activity);
                    services.get(SYNC_ID_RTM).synchronize(activity);
                }

                closeControllers();
            }
        }, "sync").start();
    }

    /** Clears tokens if services are disabled */
    public static void synchronizerStatusUpdated(Activity activity) {
        // do nothing
    }

    // --- package helpers

    /** Service map */
    private static Map<Integer, SynchronizationService> services =
        new HashMap<Integer, SynchronizationService>();
    static {
        services.put(SYNC_ID_RTM, new RTMSyncService(SYNC_ID_RTM));
    }

    static SyncDataController getSyncController() {
        return syncController;
    }

    static TaskController getTaskController() {
        return taskController;
    }

    // --- controller stuff
    private static SyncDataController syncController = null;
    private static TaskController taskController = null;

    private static void openControllers(Activity activity) {
        if(syncController == null) {
            syncController = new SyncDataController(activity);
            syncController.open();
        }

        if(taskController == null) {
            taskController = new TaskController(activity);
            taskController.open();
        }
    }

    private static void closeControllers() {
        if(syncController != null) {
            syncController.close();
            syncController = null;
        }

        if(taskController != null) {
            taskController.close();
            taskController = null;
        }
    }
}
