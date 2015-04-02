package org.tasks;

import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.reminders.NotificationFragment;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.utility.Constants;

import org.tasks.injection.ForApplication;
import org.tasks.receivers.CompleteTaskReceiver;
import org.tasks.receivers.FirstLaunchReceiver;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Broadcaster {

    public static final String BROADCAST_IN_APP_NOTIFY = Constants.PACKAGE + ".IN_APP_NOTIFY"; //$NON-NLS-1$

    private final Context context;

    @Inject
    public Broadcaster(@ForApplication Context context) {
        this.context = context;
    }

    public void toggleCompletedState(long taskId) {
        completeTask(taskId, true);
    }

    public void completeTask(long taskId) {
        completeTask(taskId, false);
    }

    private void completeTask(final long taskId, final boolean flipState) {
        sendOrderedBroadcast(new Intent(context, CompleteTaskReceiver.class) {{
            putExtra(CompleteTaskReceiver.TASK_ID, taskId);
            putExtra(CompleteTaskReceiver.TOGGLE_STATE, flipState);
        }});
    }

    public void requestNotification(final long taskId, final Intent intent, final int type,
                                    final String title, final String text, final int ringTimes) {
        sendOrderedBroadcast(new Intent(BROADCAST_IN_APP_NOTIFY) {{
            putExtra(Notifications.EXTRAS_NOTIF_ID, (int) taskId);
            putExtra(NotificationFragment.TOKEN_ID, taskId);
            putExtra(Notifications.EXTRAS_CUSTOM_INTENT, intent);
            putExtra(Notifications.EXTRAS_TYPE, type);
            putExtra(Notifications.EXTRAS_TITLE, title);
            putExtra(Notifications.EXTRAS_TEXT, text);
            putExtra(Notifications.EXTRAS_RING_TIMES, ringTimes);
        }}, AstridApiConstants.PERMISSION_READ);
    }

    public void requestNotification(final long alarmId, final long taskId) {
        sendOrderedBroadcast(new Intent(context, Notifications.class) {{
            setAction("ALARM" + alarmId); //$NON-NLS-1$
            putExtra(Notifications.ID_KEY, taskId);
            putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_ALARM);
        }});
    }

    public void taskCompleted(final long id) {
        sendOrderedBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_COMPLETED) {{
            putExtra(AstridApiConstants.EXTRAS_TASK_ID, id);
        }});
    }

    public void refresh() {
        context.sendBroadcast(new Intent(AstridApiConstants.BROADCAST_EVENT_REFRESH));
    }

    private void sendOrderedBroadcast(Intent intent) {
        sendOrderedBroadcast(intent, null);
    }

    void sendOrderedBroadcast(Intent intent, String permissions) {
        context.sendOrderedBroadcast(intent, permissions);
    }

    public void firstLaunch() {
        context.sendBroadcast(new Intent(context, FirstLaunchReceiver.class));
    }
}
