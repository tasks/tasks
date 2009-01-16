package com.timsu.astrid.sync;

import java.util.Date;

import android.app.Activity;
import android.content.Context;

import com.timsu.astrid.data.alerts.AlertController;
import com.timsu.astrid.data.sync.SyncDataController;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.utilities.Preferences;

public class Synchronizer {

    private static final int SYNC_ID_RTM = 1;

    // --- public interface

    public interface SynchronizerListener {
        void onSynchronizerFinished(int numServicesSynced);
    }

    /** Synchronize all activated sync services */
    public static void synchronize(Activity activity, SynchronizerListener listener) {
        currentStep = ServiceWrapper._FIRST_SERVICE.ordinal();
        servicesSynced = 0;
        callback = listener;
        continueSynchronization(activity);
    }


    /** Clears tokens if services are disabled */
    public static void clearUserData(Activity activity) {
        for(ServiceWrapper serviceWrapper : ServiceWrapper.values()) {
            if(serviceWrapper.isActivated(activity)) {
                serviceWrapper.service.clearPersonalData(activity);
            }
        }
    }

    // --- internal synchronization logic

    /** Synchronization Services enumeration
     *    note that id must be kept constant!
     * @author timsu
     *
     */
    private enum ServiceWrapper {
        _FIRST_SERVICE(null) { // must be first entry
            @Override
            boolean isActivated(Context arg0) {
                return false;
            }
        },

        RTM(new RTMSyncService(SYNC_ID_RTM)) {
            @Override
            boolean isActivated(Context context) {
                return Preferences.shouldSyncRTM(context);
            }
        },

        _LAST_SERVICE(null) { // must be last entry
            @Override
            boolean isActivated(Context arg0) {
                return false;
            }
        };

        private SynchronizationService service;

        private ServiceWrapper(SynchronizationService service) {
            this.service = service;
        }

        abstract boolean isActivated(Context context);
    }

    // Internal state for the synchronization process

    /** Current step in the sync process */
    private static int currentStep;

    /** # of services synchronized */
    private static int servicesSynced;

    /** On finished callback */
    private static SynchronizerListener callback;


    /** Called to do the next step of synchronization. Run me on the UI thread! */
    static void continueSynchronization(Activity activity) {
        ServiceWrapper serviceWrapper =
            ServiceWrapper.values()[currentStep];
        currentStep++;
        switch(serviceWrapper) {
        case _FIRST_SERVICE:
            continueSynchronization(activity);
            break;
        case RTM:
            if(Preferences.shouldSyncRTM(activity)) {
                servicesSynced++;
                serviceWrapper.service.synchronizeService(activity);
            } else {
                continueSynchronization(activity);
            }
            break;
        case _LAST_SERVICE:
            finishSynchronization(activity);
        }
    }

    /** Called at the end of sync. */
    private static void finishSynchronization(final Activity activity) {
        closeControllers();
        Preferences.setSyncLastSync(activity, new Date());
        if(callback != null)
            callback.onSynchronizerFinished(servicesSynced);
    }

    // --- package helpers

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

    static AlertController getAlertController(Activity activity) {
        if(alertController == null) {
            alertController = new AlertController(activity);
            alertController.open();
        }
        return alertController;
    }

    // --- controller stuff
    private static SyncDataController syncController = null;
    private static TaskController taskController = null;
    private static TagController tagController = null;
    private static AlertController alertController = null;

    private static void closeControllers() {
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

        if(alertController != null) {
            alertController.close();
            alertController = null;
        }
    }
}
