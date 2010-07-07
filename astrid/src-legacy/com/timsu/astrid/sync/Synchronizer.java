/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
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
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.utilities.AstridUtilities;
import com.timsu.astrid.utilities.Preferences;

/**
 * Synchronizer is a class that manages a synchronization lifecycle. You would
 * use it as follows.
 * <p>
 * <pre>Synchronizer synchronizer = new Synchronizer(...);
 * synchronizer.synchronize();</pre>
 *
 * @author Tim Su
 *
 */
public class Synchronizer {

    /** identifier for the RTM sync provider */
    private static final int SYNC_ID_RTM = 1;

    // --- public interface

    /** Synchronize all tasks */
    public Synchronizer(boolean isService) {
        this.isService = isService;
        singleTaskForSync = null;
    }

    /** Synchronize a specific task only */
    public Synchronizer(TaskIdentifier task) {
        isService = false;
        singleTaskForSync = task;
    }

    public interface SynchronizerListener {
        void onSynchronizerFinished(int numServicesSynced);
    }

    /** Synchronize all activated sync services. */
    public synchronized void synchronize(Context context,
            SynchronizerListener listener) {
        currentStep = ServiceWrapper._FIRST_SERVICE.ordinal();
        servicesSynced = 0;
        callback = listener;

        continueSynchronization(context);
    }


    /** Clears tokens of activated services */
    public static void clearUserData(Activity activity) {
        Synchronizer synchronizer = new Synchronizer(false);
        for(ServiceWrapper serviceWrapper : ServiceWrapper.values()) {
            if(serviceWrapper.isActivated(activity)) {
                serviceWrapper.service.synchronizer = synchronizer;
                serviceWrapper.service.clearPersonalData(activity);
            }
        }
        synchronizer.closeControllers();
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

        RTM(new RTMSyncProvider(SYNC_ID_RTM)) {
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

        private SynchronizationProvider service;

        private ServiceWrapper(SynchronizationProvider service) {
            this.service = service;
        }

        abstract boolean isActivated(Context context);
    }

    // Internal state for the synchronization process

    /** Current step in the sync process */
    private int currentStep = 0;

    /** # of services synchronized */
    private int servicesSynced = 0;

    /** On finished callback */
    private SynchronizerListener callback = null;

    /** Whether this sync is initiated by a background service */
    private final boolean isService;

    /** The single task to synchronize, if applicable */
    private final TaskIdentifier singleTaskForSync;

    boolean isService() {
        return isService;
    }

    TaskIdentifier getSingleTaskForSync() {
        return singleTaskForSync;
    }

    /** Called to do the next step of synchronization. */
    void continueSynchronization(Context context) {
    	try {
    		if(currentStep >= ServiceWrapper.values().length)
    			currentStep = ServiceWrapper.values().length - 1;

	        ServiceWrapper serviceWrapper =
	            ServiceWrapper.values()[currentStep];
	        currentStep++;
	        switch(serviceWrapper) {
	        case _FIRST_SERVICE:
	            continueSynchronization(context);
	            break;
	        case RTM:
	            if(serviceWrapper.isActivated(context)) {
	                servicesSynced++;
	                serviceWrapper.service.synchronizeService(context, this);
	            } else {
	                continueSynchronization(context);
	            }
	            break;
	        case _LAST_SERVICE:
	            finishSynchronization(context);
	        }
    	} catch (Exception e) {
    		Log.e("sync", "Error continuing synchronization", e);
    		AstridUtilities.reportFlurryError("sync-continue", e);
    		finishSynchronization(context);
    	}
    }

    /** Called at the end of sync. */
    private void finishSynchronization(final Context context) {
        closeControllers();
        if(callback != null)
            callback.onSynchronizerFinished(servicesSynced);

        if(getSingleTaskForSync() == null)
            Preferences.setSyncLastSync(context, new Date());
        if(!isService) {
//            TaskListSubActivity.shouldRefreshTaskList = true;
        }

        Log.i("sync", "Synchronization Service Finished");
    }

    // --- controller stuff

    private final ControllerWrapper<SyncDataController> syncController =
        new ControllerWrapper<SyncDataController>(SyncDataController.class);
    private final ControllerWrapper<TaskController> taskController =
        new ControllerWrapper<TaskController>(TaskController.class);
    private final ControllerWrapper<TagController> tagController =
        new ControllerWrapper<TagController>(TagController.class);
    private final ControllerWrapper<AlertController> alertController =
        new ControllerWrapper<AlertController>(AlertController.class);

    private static class ControllerWrapper<TYPE extends AbstractController> {
        TYPE controller;
        Class<TYPE> typeClass;
        boolean override;

        public ControllerWrapper(Class<TYPE> cls) {
            override = false;
            controller = null;
            typeClass = cls;
        }

        @SuppressWarnings("unchecked")
        public TYPE get(Context context) {
            if(controller == null) {
                try {
                    controller = (TYPE) typeClass.getConstructors()[0].newInstance(context);
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
            if(controller != null && !override)
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

    SyncDataController getSyncController(Context context) {
        return syncController.get(context);
    }

    TaskController getTaskController(Context context) {
        return taskController.get(context);
    }

    TagController getTagController(Context context) {
        return tagController.get(context);
    }

    AlertController getAlertController(Context context) {
        return alertController.get(context);
    }

    public void setTaskController(TaskController taskController) {
        this.taskController.set(taskController);
    }

    public void setTagController(TagController tagController) {
        this.tagController.set(tagController);
    }

    private void closeControllers() {
        syncController.close();
        taskController.close();
        tagController.close();
        alertController.close();
    }
}
