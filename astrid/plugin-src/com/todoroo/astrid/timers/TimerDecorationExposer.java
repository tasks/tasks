/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers;

import android.graphics.Color;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.RemoteViews;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.api.TaskDecorationExposer;
import com.todoroo.astrid.data.Task;

/**
 * Exposes {@link TaskDecoration} for timers
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TimerDecorationExposer implements TaskDecorationExposer {

    private static final int TIMING_BG_COLOR = Color.argb(200, 220, 50, 0);

    @Override
    public TaskDecoration expose(Task task) {
        if(task == null || (task.getValue(Task.ELAPSED_SECONDS) == 0 &&
                task.getValue(Task.TIMER_START) == 0))
            return null;

        TaskDecoration decoration;
        RemoteViews remoteViews = new RemoteViews(ContextManager.getContext().getPackageName(),
                R.layout.timer_decoration);
        decoration = new TaskDecoration(remoteViews,
                TaskDecoration.POSITION_LEFT, 0);

        long elapsed = task.getValue(Task.ELAPSED_SECONDS) * 1000L;
        if(task.getValue(Task.TIMER_START) != 0) {
            decoration.color = TIMING_BG_COLOR;
            elapsed += DateUtilities.now() - task.getValue(Task.TIMER_START);
            decoration.decoration.setChronometer(R.id.timer, SystemClock.elapsedRealtime() -
                    elapsed, null, true);
            decoration.decoration.setViewVisibility(R.id.timer, View.VISIBLE);
            decoration.decoration.setViewVisibility(R.id.label, View.GONE);
        } else {
            // if timer is not started, make the chronometer just a text label,
            // since we don't want the time to be displayed relative to elapsed
            String format = buildFormat(elapsed);
            decoration.color = 0;
            decoration.decoration.setTextViewText(R.id.label, format);
            decoration.decoration.setViewVisibility(R.id.timer, View.GONE);
            decoration.decoration.setViewVisibility(R.id.label, View.VISIBLE);
        }

        return decoration;
    }

    private String buildFormat(long elapsed) {
        return DateUtils.formatElapsedTime(elapsed / 1000L);
    }

}
