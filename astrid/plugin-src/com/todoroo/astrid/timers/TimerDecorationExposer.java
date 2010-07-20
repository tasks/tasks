/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.service.TaskService;

/**
 * Exposes {@link TaskDecoration} for timers
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TimerDecorationExposer extends BroadcastReceiver {

    private static final int TASK_BG_COLOR = Color.argb(200, 220, 50, 0);

    static TaskService staticTaskService;
    private static HashMap<Long, TaskDecoration> decorations =
        new HashMap<Long, TaskDecoration>();
    private static HashMap<Long, Timer> timers =
        new HashMap<Long, Timer>();

    @Autowired
    private TaskService taskService;

    private static class TimerTimerTask extends TimerTask {
        int time;
        RemoteViews remoteViews;

        public TimerTimerTask(int time, RemoteViews remoteViews) {
            super();
            this.time = time;
            this.remoteViews = remoteViews;
        }

        @Override
        public void run() {
            time++;
            int seconds = time % 60;
            int minutes = time / 60;
            if(minutes > 59) {
                int hours = minutes / 60;
                minutes %= 60;
                remoteViews.setTextViewText(R.id.timer,
                        String.format("%02d:%02d:%02d", //$NON-NLS-1$
                                hours, minutes, seconds));
            } else {
                remoteViews.setTextViewText(R.id.timer,
                        String.format("%02d:%02d", //$NON-NLS-1$
                                minutes, seconds));
            }
        }
    }

    public void removeFromCache(long taskId) {
        decorations.remove(taskId);
        timers.get(taskId).cancel();
        timers.remove(taskId);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        synchronized(TimerDecorationExposer.class) {
            if(staticTaskService == null) {
                DependencyInjectionService.getInstance().inject(this);
                staticTaskService = taskService;
            } else {
                taskService = staticTaskService;
            }
        }

        TaskDecoration decoration;

        if(!decorations.containsKey(taskId)) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.timer_decoration);
            decoration = new TaskDecoration(remoteViews,
                    TaskDecoration.POSITION_LEFT, TASK_BG_COLOR);
            decorations.put(taskId, decoration);
            Timer timer = new Timer();
            timers.put(taskId, timer);
            timer.scheduleAtFixedRate(new TimerTimerTask(0,
                    remoteViews), 0, 1000L);
        } else {
            decoration = decorations.get(taskId);
        }

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DECORATIONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, TimerPlugin.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, decoration);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
