package org.tasks.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao;

import org.tasks.LocalBroadcastManager;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.DefaultFilterProvider;

import javax.inject.Inject;

import me.leolin.shortcutbadger.ShortcutBadger;
import timber.log.Timber;

@ApplicationScope
public class Badger {

    private final Context context;
    private final DefaultFilterProvider defaultFilterProvider;
    private final TaskDao taskDao;
    private final LocalBroadcastManager localBroadcastManager;

    private boolean enabled;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            publishCount();
        }
    };

    @Inject
    public Badger(@ForApplication Context context, DefaultFilterProvider defaultFilterProvider,
                  TaskDao taskDao, LocalBroadcastManager localBroadcastManager) {
        this.context = context;
        this.defaultFilterProvider = defaultFilterProvider;
        this.taskDao = taskDao;
        this.localBroadcastManager = localBroadcastManager;
    }

    public void setEnabled(boolean newValue) {
        try {
            if (newValue) {
                localBroadcastManager.registerRefreshReceiver(receiver);
                publishCount();
            } else if (enabled) {
                localBroadcastManager.unregisterReceiver(receiver);
                ShortcutBadger.removeCount(context);
            }
            enabled = newValue;
        } catch (Exception e) {
            Timber.e(e, e.getMessage());
        }
    }

    private void publishCount() {
        Filter badgeFilter = defaultFilterProvider.getBadgeFilter();
        int count = taskDao.count(badgeFilter);
        ShortcutBadger.applyCount(context, count);
    }
}
