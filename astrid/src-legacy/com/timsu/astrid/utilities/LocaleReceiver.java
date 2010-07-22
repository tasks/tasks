package com.timsu.astrid.utilities;

import java.util.HashSet;
import java.util.LinkedList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.todoroo.astrid.activity.LocaleEditAlerts;

/**
 * Receiver is activated when Locale conditions are triggered
 *
 * @author timsu
 *
 */
public class LocaleReceiver extends BroadcastReceiver {

    /** minimum amount of time between two notifications */
    private static final long MIN_NOTIFY_INTERVAL = 12*3600*1000L;

    @Override
    /** Called when the system is started up */
    public void onReceive(Context context, Intent intent) {
    	try {
	    	if (LocaleEditAlerts.ACTION_LOCALE_ALERT.equals(intent.getAction())) {
				final long tagId = intent.getLongExtra(LocaleEditAlerts.KEY_TAG_ID, 0);
				final String tagName = intent.getStringExtra(LocaleEditAlerts.KEY_TAG_NAME);
				if(tagId == 0) {
					Log.w("astrid-locale", "Invalid tag identifier in alert");
					return;
				}

				// check if we've already made a notification recently
			    if(System.currentTimeMillis() - Preferences.
			            getLocaleLastAlertTime(context, tagId) < MIN_NOTIFY_INTERVAL) {
			        Log.w("astrid-locale", "Too soon, need " + (MIN_NOTIFY_INTERVAL - System.currentTimeMillis() + Preferences.
                        getLocaleLastAlertTime(context, tagId))/1000L + " more seconds");
			        return;
				}

				// find out if we have active tasks with this tag
				TaskController taskController = new TaskController(context);
				taskController.open();
				TagController tagController = new TagController(context);
				tagController.open();
				try {
					HashSet<TaskIdentifier> activeTasks = taskController.getActiveVisibleTaskIdentifiers();
					LinkedList<TaskIdentifier> tasks = tagController.getTaggedTasks(
							new TagIdentifier(tagId));
//					int count = TagListSubActivity.countActiveTasks(activeTasks, tasks);
					int count = 0;
					if(count > 0) {
						Resources r = context.getResources();
//						String reminder = r.getString(R.string.notif_tagNotification).
						String reminder = "$NUM of $TAG".
						    replace("$NUM", r.getQuantityString(R.plurals.Ntasks, count, count)).
						    replace("$TAG", tagName);
//						ReminderService.showTagNotification(context, tagId, reminder);

						Preferences.setLocaleLastAlertTime(context, tagId,
						        System.currentTimeMillis());
					} else {
					    Log.w("astrid-locale", "Locale Notification, but no tasks to show");
					}
				} finally {
					taskController.close();
					tagController.close();
				}
	    	}
    	} catch (Exception e) {
    		Log.e("astrid-locale-rx", "Error receiving intent", e);
    	}
    }

}
