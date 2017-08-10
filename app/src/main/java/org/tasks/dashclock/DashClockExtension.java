package org.tasks.dashclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.google.android.apps.dashclock.api.ExtensionData;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;

import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.injection.InjectingApplication;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class DashClockExtension extends com.google.android.apps.dashclock.api.DashClockExtension {

    @Inject DefaultFilterProvider defaultFilterProvider;
    @Inject TaskDao taskDao;
    @Inject Preferences preferences;
    @Inject LocalBroadcastManager localBroadcastManager;

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        ((InjectingApplication) getApplication())
                .getComponent()
                .inject(this);

        localBroadcastManager.registerRefreshReceiver(refreshReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        localBroadcastManager.unregisterReceiver(refreshReceiver);
    }

    @Override
    protected void onUpdateData(int i) {
        refresh();
    }

    private void refresh() {
        if (preferences.hasPurchase(R.string.p_purchased_dashclock)) {
            final String filterPreference = preferences.getStringValue(R.string.p_dashclock_filter);
            Filter filter = defaultFilterProvider.getFilterFromPreference(filterPreference);

            int count = taskDao.count(filter);

            if (count == 0) {
                publish(null);
            } else {
                Intent clickIntent = new Intent(this, TaskListActivity.class);
                clickIntent.putExtra(TaskListActivity.LOAD_FILTER, filterPreference);
                ExtensionData extensionData = new ExtensionData()
                        .visible(true)
                        .icon(R.drawable.ic_check_white_24dp)
                        .status(Integer.toString(count))
                        .expandedTitle(getString(R.string.task_count, count))
                        .expandedBody(filter.listingTitle)
                        .clickIntent(clickIntent);
                if (count == 1) {
                    List<Task> tasks = taskDao.query(filter);
                    if (!tasks.isEmpty()) {
                        extensionData.expandedTitle(tasks.get(0).getTitle());
                    }
                }
                publish(extensionData);
            }
        } else {
            publish(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.ic_check_white_24dp)
                    .status(getString(R.string.buy))
                    .expandedTitle(getString(R.string.buy_dashclock_extension))
                    .clickIntent(new Intent(this, DashClockSettings.class)));
        }
    }

    private void publish(ExtensionData data) {
        try {
            publishUpdate(data);
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
    }
}
