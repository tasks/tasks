package org.tasks.receivers;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;

import org.tasks.Broadcaster;
import org.tasks.BuildConfig;
import org.tasks.analytics.Tracker;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.preferences.DefaultFilterProvider;

import javax.inject.Inject;

import timber.log.Timber;

@ApplicationScope
public class TeslaUnreadReceiver extends InjectingBroadcastReceiver {

    private static final String TESLA_URI = "content://com.teslacoilsw.notifier/unread_count";
    private static final String TESLA_TAG = BuildConfig.APPLICATION_ID + "/com.todoroo.astrid.activity.TaskListActivity";

    private final Context context;
    private final DefaultFilterProvider defaultFilterProvider;
    private final TaskDao taskDao;
    private final Tracker tracker;
    private final Broadcaster broadcaster;

    private boolean enabled;

    @Inject
    public TeslaUnreadReceiver(@ForApplication Context context, DefaultFilterProvider defaultFilterProvider,
                               TaskDao taskDao, Tracker tracker, Broadcaster broadcaster) {
        this.context = context;
        this.defaultFilterProvider = defaultFilterProvider;
        this.taskDao = taskDao;
        this.tracker = tracker;
        this.broadcaster = broadcaster;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Filter defaultFilter = defaultFilterProvider.getDefaultFilter();
        publishCount(taskDao.count(defaultFilter));
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }

    public void setEnabled(boolean newValue) {
        try {
            if (newValue) {
                context.registerReceiver(this, new IntentFilter(AstridApiConstants.BROADCAST_EVENT_REFRESH));
                broadcaster.refresh();
            } else if (enabled) {
                context.unregisterReceiver(this);
                publishCount(0);
            }
            enabled = newValue;
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
    }

    private void publishCount(int count) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("tag", TESLA_TAG);
            contentValues.put("count", count);
            context.getContentResolver().insert(Uri.parse(TESLA_URI), contentValues);
        } catch (IllegalArgumentException ex) {
            /* Fine, TeslaUnread is not installed. */
        } catch (Exception e) {
            tracker.reportException(e);
        }
    }
}
