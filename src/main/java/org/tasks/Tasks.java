package org.tasks;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.util.Log;

import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksMetadataService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;

import org.tasks.analytics.Tracker;
import org.tasks.billing.IabHelper;
import org.tasks.billing.IabResult;
import org.tasks.billing.Inventory;
import org.tasks.filters.FilterCounter;
import org.tasks.injection.ApplicationComponent;
import org.tasks.injection.InjectingApplication;
import org.tasks.preferences.Preferences;
import org.tasks.receivers.TeslaUnreadReceiver;
import org.tasks.sync.SyncThrottle;

import javax.inject.Inject;

import timber.log.Timber;

import static org.tasks.preferences.BasicPreferences.toggleTasker;

@SuppressWarnings("UnusedDeclaration")
public class Tasks extends InjectingApplication implements IabHelper.OnIabSetupFinishedListener, IabHelper.QueryInventoryFinishedListener {

    @Inject Database database;
    @Inject TaskDao taskDao;
    @Inject MetadataDao metadataDao;
    @Inject TagDataDao tagDataDao;
    @Inject StoreObjectDao storeObjectDao;
    @Inject UserActivityDao userActivityDao;
    @Inject TaskAttachmentDao taskAttachmentDao;
    @Inject TaskListMetadataDao taskListMetadataDao;
    @Inject TaskService taskService;
    @Inject SyncV2Service syncV2Service;
    @Inject GtasksPreferenceService gtasksPreferenceService;
    @Inject GtasksListService gtasksListService;
    @Inject GtasksMetadataService gtasksMetadataService;
    @Inject GtasksTaskListUpdater gtasksTaskListUpdater;
    @Inject GtasksSyncService gtasksSyncService;
    @Inject TagService tagService;
    @Inject Broadcaster broadcaster;
    @Inject FilterCounter filterCounter;
    @Inject SyncThrottle syncThrottle;
    @Inject Preferences preferences;
    @Inject Tracker tracker;
    @Inject TeslaUnreadReceiver teslaUnreadReceiver;
    @Inject IabHelper iabHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new ErrorReportingTree());
        }

        tracker.setTrackingEnabled(preferences.isTrackingEnabled());

        iabHelper.startSetup(this);

        teslaUnreadReceiver.setEnabled(preferences.getBoolean(R.string.p_tesla_unread_enabled, false));

        try {
            toggleTasker(this, preferences.getBoolean(R.string.p_tasker_enabled, false));
        } catch(Exception e) {
            Timber.e(e, e.getMessage());
            tracker.reportException(e);
        }
    }

    @Override
    public void onIabSetupFinished(IabResult result) {
        if (result.isSuccess()) {
            Timber.d("IAB setup successful");
            iabHelper.queryInventoryAsync(this);
        } else {
            Timber.e(result.getMessage());
        }
    }

    @Override
    public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
        if (result.isFailure()) {
            Timber.e("Query inventory failed: %s", result);
        } else {
            if (inventory.hasPurchase(getString(R.string.sku_tesla_unread))) {
                preferences.setBoolean(R.string.p_purchased_tesla_unread, false);
            }
            if (inventory.hasPurchase(getString(R.string.sku_tasker))) {
                preferences.setBoolean(R.string.p_purchased_tasker, false);
            }
        }
    }

    private static class ErrorReportingTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority < Log.WARN) {
                return;
            }
            if (priority == Log.ERROR) {
                if (t == null) {
                    Log.e(tag, message);
                } else {
                    Log.e(tag, message, t);
                }
            } else if(priority == Log.WARN) {
                if (t == null) {
                    Log.w(tag, message);
                } else {
                    Log.w(tag, message, t);
                }
            }
        }
    }

    @Override
    protected void inject(ApplicationComponent component) {
        component.inject(this);
    }
}
