package com.todoroo.astrid.locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.ShortcutActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.service.TaskService;
import com.todoroo.astrid.utility.Constants;
import com.todoroo.astrid.utility.Preferences;

/**
 * Receiver is activated when Locale conditions are triggered
 *
 * @author timsu
 *
 */
public class LocaleReceiver extends BroadcastReceiver {

    @Autowired
    private TaskService taskService;

    /**
     * Create a preference key for storing / retrieving last interval time
     * @param filterTitle
     * @param interval
     * @return
     */
    private String makePreferenceKey(String filterTitle, int interval) {
        return "LOCALE:" + filterTitle + interval; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    /** Called when the system is started up */
    public void onReceive(Context context, Intent intent) {
    	try {
    	    if (com.twofortyfouram.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
	    	    final String title = intent.getStringExtra(LocaleEditAlerts.KEY_FILTER_TITLE);
				final String sql = intent.getStringExtra(LocaleEditAlerts.KEY_SQL);
				final int interval = intent.getIntExtra(LocaleEditAlerts.KEY_INTERVAL, 24*3600);

				if(TextUtils.isEmpty(title) || TextUtils.isEmpty(sql) ||
				        sql.contains("--") || sql.contains(";") || interval == 0)
				    return;

				// check if we've already made a notification recently
				String preferenceKey = makePreferenceKey(title, interval);
				long lastNotifyTime = Preferences.getLong(preferenceKey, 0);
				if(DateUtilities.now() - lastNotifyTime < interval * 1000L) {
			        Log.i("astrid-locale", title + ": Too soon, need " + (interval
			                - (DateUtilities.now() - lastNotifyTime)/1000) + " more seconds");
			        return;
				}

				// find out if we have active tasks with this tag
				DependencyInjectionService.getInstance().inject(this);
				Filter filter = new Filter(title, title, null, null);
				filter.sqlQuery = sql;
				TodorooCursor<Task> cursor = taskService.fetchFiltered(filter, Task.ID);
				try {
				    if(cursor.getCount() == 0)
				        return;
					Resources r = context.getResources();
					String reminder = r.getString(R.string.locale_notification).
						    replace("$NUM", r.getQuantityString(R.plurals.Ntasks,
						            cursor.getCount(), cursor.getCount())).
						    replace("$FILTER", title);

					// show a reminder
					String notificationTitle = r.getString(R.string.locale_edit_alerts_title);
		            Intent notifyIntent = ShortcutActivity.createIntent(filter);
		            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		            Notifications.showNotification(Constants.NOTIFICATION_LOCALE,
		                    notifyIntent, 0, notificationTitle, reminder, false);

					Preferences.setLong(preferenceKey, DateUtilities.now());
				} finally {
					cursor.close();
				}
	    	}
    	} catch (Exception e) {
    	    if(Constants.DEBUG)
    	        Log.i("astrid-locale-rx", "Error receiving intent", e);
    	}
    }

}
