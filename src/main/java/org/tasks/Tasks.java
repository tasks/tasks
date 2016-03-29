package org.tasks;

import android.content.res.Configuration;
import android.util.Log;

import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskAttachmentDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.TaskListMetadataDao;
import com.todoroo.astrid.dao.UserActivityDao;
import com.todoroo.astrid.service.SyncV2Service;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.tags.TagService;

import org.tasks.analytics.Tracker;
import org.tasks.filters.FilterCounter;
import org.tasks.injection.ApplicationComponent;
import org.tasks.injection.InjectingApplication;
import org.tasks.preferences.Preferences;
import org.tasks.sync.SyncThrottle;

import javax.inject.Inject;

import timber.log.Timber;

@SuppressWarnings("UnusedDeclaration")
public class Tasks extends InjectingApplication {

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
    @Inject TagService tagService;
    @Inject Broadcaster broadcaster;
    @Inject FilterCounter filterCounter;
    @Inject SyncThrottle syncThrottle;
    @Inject Preferences preferences;
    @Inject Tracker tracker;
    @Inject FlavorSetup flavorSetup;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new ErrorReportingTree());
        }

        tracker.setTrackingEnabled(preferences.isTrackingEnabled());

        flavorSetup.setup();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (BuildConfig.DEBUG) {
            Timber.d("current screen width: %s, smallest screen width: %s", newConfig.screenWidthDp, newConfig.smallestScreenWidthDp);
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
