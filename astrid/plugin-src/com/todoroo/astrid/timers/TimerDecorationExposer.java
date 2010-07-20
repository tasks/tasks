/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.SystemClock;
import android.widget.RemoteViews;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.model.Task;

/**
 * Exposes {@link TaskDecoration} for timers
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TimerDecorationExposer extends BroadcastReceiver {

    private static final int TIMING_BG_COLOR = Color.argb(200, 220, 50, 0);

    private static HashMap<Long, TaskDecoration> decorations =
        new HashMap<Long, TaskDecoration>();

    public static void removeFromCache(long taskId) {
        decorations.remove(taskId);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        Task task = PluginServices.getTaskService().fetchById(taskId, Task.ELAPSED_SECONDS, Task.TIMER_START);
        if(task == null || (task.getValue(Task.ELAPSED_SECONDS) == 0 &&
                task.getValue(Task.TIMER_START) == 0))
            return;

        TaskDecoration decoration;
        if(!decorations.containsKey(taskId)) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.timer_decoration);
            decoration = new TaskDecoration(remoteViews,
                    TaskDecoration.POSITION_LEFT, 0);
            decorations.put(taskId, decoration);
        } else {
            decoration = decorations.get(taskId);
        }

        long elapsed = task.getValue(Task.ELAPSED_SECONDS) * 1000L;
        if(task.getValue(Task.TIMER_START) != 0) {
            decoration.color = TIMING_BG_COLOR;
            elapsed += DateUtilities.now() - task.getValue(Task.TIMER_START);
        }

        // update timer
        decoration.decoration.setChronometer(R.id.timer, SystemClock.elapsedRealtime() -
                elapsed, null, decoration.color != 0);

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DECORATIONS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, TimerPlugin.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, decoration);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
