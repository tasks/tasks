package com.timsu.astrid.sync;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;

import com.timsu.astrid.data.sync.SyncDataController;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.utilities.Preferences;

public class Synchronizer {

    // Synchronization Service ID's
    private static final int SYNC_ID_RTM = 1;

    // --- public interface

    /** Synchronize all activated sync services */
    public static void synchronize(final Activity activity) {

        // RTM sync
        if(Preferences.shouldSyncRTM(activity)) {
            services.get(SYNC_ID_RTM).synchronizeService(activity);
        }

    }

    /** Clears tokens if services are disabled */
    public static void clearUserData(Activity activity) {
        if(Preferences.shouldSyncRTM(activity)) {
            services.get(SYNC_ID_RTM).clearPersonalData(activity);
        }
    }

    // --- package helpers

    /** Service map */
    private static Map<Integer, SynchronizationService> services =
        new HashMap<Integer, SynchronizationService>();
    static {
        services.put(SYNC_ID_RTM, new RTMSyncService(SYNC_ID_RTM));
    }

    static SyncDataController getSyncController(Activity activity) {
        if(syncController == null) {
            syncController = new SyncDataController(activity);
            syncController.open();
        }
        return syncController;
    }

    static TaskController getTaskController(Activity activity) {
        if(taskController == null) {
            taskController = new TaskController(activity);
            taskController.open();
        }
        return taskController;
    }

    static TagController getTagController(Activity activity) {
        if(tagController == null) {
            tagController = new TagController(activity);
            tagController.open();
        }
        return tagController;
    }

    // --- controller stuff
    private static SyncDataController syncController = null;
    private static TaskController taskController = null;
    private static TagController tagController = null;

    static void closeControllers() {
        if(syncController != null) {
            syncController.close();
            syncController = null;
        }

        if(taskController != null) {
            taskController.close();
            taskController = null;
        }

        if(tagController != null) {
            tagController.close();
            tagController = null;
        }
    }
}
