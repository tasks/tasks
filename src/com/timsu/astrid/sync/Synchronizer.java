package com.timsu.astrid.sync;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.timsu.astrid.data.AbstractController;
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
    public static void synchronize(Activity activity, boolean isAutoSync,
            SynchronizerListener listener) {
        currentStep = ServiceWrapper._FIRST_SERVICE.ordinal();
        servicesSynced = 0;
        autoSync = isAutoSync;
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

    /** If this synchronization was automatically initiated */
    private static boolean autoSync;


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

    /** Was this sync automatically initiated? */
    static boolean isAutoSync() {
        return autoSync;
    }

    // --- controller stuff

    private static class ControllerWrapper<TYPE extends AbstractController> {
        TYPE controller;
        Class<TYPE> typeClass;
        boolean override;

        public ControllerWrapper(Class<TYPE> cls) {
            override = false;
            controller = null;
            typeClass = cls;
        }

        public TYPE get(Activity activity) {
            if(controller == null) {
                try {
                    controller = typeClass.getConstructors()[0].newInstance(
                            activity);
                } catch (IllegalArgumentException e) {
                    Log.e(getClass().getSimpleName(), e.toString());
                } catch (SecurityException e) {
                    Log.e(getClass().getSimpleName(), e.toString());
                } catch (InstantiationException e) {
                    Log.e(getClass().getSimpleName(), e.toString());
                } catch (IllegalAccessException e) {
                    Log.e(getClass().getSimpleName(), e.toString());
                } catch (InvocationTargetException e) {
                    Log.e(getClass().getSimpleName(), e.toString());
                }
                controller.open();
            }
            return controller;
        }

        public void set(TYPE newController) {
            close();

            override = newController != null;
            controller = newController;
        }

        public void close() {
            if(controller != null && !override) {
                controller.close();
                controller = null;
            }
        }
    }

   private static ControllerWrapper<SyncDataController> syncController =
        new ControllerWrapper<SyncDataController>(SyncDataController.class);
    private static ControllerWrapper<TaskController> taskController =
        new ControllerWrapper<TaskController>(TaskController.class);
    private static ControllerWrapper<TagController> tagController =
        new ControllerWrapper<TagController>(TagController.class);
    private static ControllerWrapper<AlertController> alertController =
        new ControllerWrapper<AlertController>(AlertController.class);

    static SyncDataController getSyncController(Activity activity) {
        return syncController.get(activity);
    }

    static TaskController getTaskController(Activity activity) {
        return taskController.get(activity);
    }

    static TagController getTagController(Activity activity) {
        return tagController.get(activity);
    }

    static AlertController getAlertController(Activity activity) {
        return alertController.get(activity);
    }

    public static void setTaskController(TaskController taskController) {
        Synchronizer.taskController.set(taskController);
    }

    public static void setTagController(TagController tagController) {
        Synchronizer.tagController.set(tagController);
    }

    private static void closeControllers() {
        syncController.close();
        taskController.close();
        tagController.close();
        alertController.close();
    }
}
