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

import org.tasks.R;
import org.tasks.injection.InjectingDashClockExtension;
import org.tasks.injection.ServiceComponent;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.Preferences;

import java.util.List;

import javax.inject.Inject;

public class DashClockExtension extends InjectingDashClockExtension {

    @Inject DefaultFilterProvider defaultFilterProvider;
    @Inject TaskDao taskDao;
    @Inject Preferences preferences;

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        registerReceiver(refreshReceiver, new IntentFilter(AstridApiConstants.BROADCAST_EVENT_REFRESH));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(refreshReceiver);
    }

    @Override
    protected void onUpdateData(int i) {
        refresh();
    }

    @Override
    protected void inject(ServiceComponent component) {
        component.inject(this);
    }

    private void refresh() {
        if (preferences.hasPurchase(R.string.p_purchased_dashclock)) {
            final String filterPreference = preferences.getStringValue(R.string.p_dashclock_filter);
            Filter filter = defaultFilterProvider.getFilterFromPreference(filterPreference);

            int count = taskDao.count(filter);

            if (count == 0) {
                publishUpdate(null);
            } else {
                ExtensionData extensionData = new ExtensionData()
                        .visible(true)
                        .icon(R.drawable.ic_check_white_24dp)
                        .status(Integer.toString(count))
                        .expandedTitle(getString(R.string.task_count, count))
                        .expandedBody(filter.listingTitle)
                        .clickIntent(new Intent(this, TaskListActivity.class) {{
                            putExtra(TaskListActivity.LOAD_FILTER, filterPreference);
                        }});
                if (count == 1) {
                    List<Task> tasks = taskDao.query(filter);
                    if (!tasks.isEmpty()) {
                        extensionData.expandedTitle(tasks.get(0).getTitle());
                    }
                }
                publishUpdate(extensionData);
            }
        } else {
            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.ic_check_white_24dp)
                    .status(getString(R.string.buy))
                    .expandedTitle(getString(R.string.buy_dashclock_extension))
                    .clickIntent(new Intent(this, DashClockSettings.class)));
        }
    }
}
