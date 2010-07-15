/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.repeats;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.WeekdayNum;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.TaskDetail;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.TaskService;

/**
 * Exposes {@link TaskDetail} for tags, i.e. "Tags: frogs, animals"
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class RepeatDetailExposer extends BroadcastReceiver {

    @Autowired
    TaskService taskService;

    @Override
    public void onReceive(Context context, Intent intent) {
        // get tags associated with this task
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        DependencyInjectionService.getInstance().inject(this);
        Task task = taskService.fetchById(taskId, Task.FLAGS, Task.RECURRENCE);
        if(task == null)
            return;

        Resources r = context.getResources();

        String recurrence = task.getValue(Task.RECURRENCE);
        if(recurrence != null && recurrence.length() > 0) {
            RRule rrule;
            try {
                rrule = new RRule(recurrence);
            } catch (ParseException e) {
                return;
            }
            String interval;
            switch(rrule.getFreq()) {
            case HOURLY:
                interval = r.getQuantityString(R.plurals.DUt_hours, rrule.getInterval(),
                        rrule.getInterval());
                break;
            case DAILY:
                interval = r.getQuantityString(R.plurals.DUt_days, rrule.getInterval(),
                        rrule.getInterval());
                break;
            case WEEKLY:
                interval = r.getQuantityString(R.plurals.DUt_weeks, rrule.getInterval(),
                        rrule.getInterval());
                break;
            case MONTHLY:
                interval = r.getQuantityString(R.plurals.DUt_months, rrule.getInterval(),
                        rrule.getInterval());
                break;
            default:
                // not designed to be used, only a fail-safe
                interval = rrule.getInterval() + "-" + rrule.getFreq().name(); //$NON-NLS-1$
            }

            interval = "<b>" + interval + "</b>";  //$NON-NLS-1$//$NON-NLS-2$
            if(rrule.getFreq() == Frequency.WEEKLY) {
                List<WeekdayNum> byDay = rrule.getByDay();
                if(byDay.size() > 0) {
                    StringBuilder byDayString = new StringBuilder();
                    DateFormatSymbols dfs = new DateFormatSymbols();
                    String[] weekdays = dfs.getShortWeekdays();
                    for(int i = 0; i < byDay.size(); i++) {
                        byDayString.append(weekdays[byDay.get(i).wday.javaDayNum]);
                        if(i < byDay.size() - 1)
                        byDayString.append(", "); //$NON-NLS-1$
                    }
                    interval = r.getString(R.string.repeat_detail_byday).replace("$I",  //$NON-NLS-1$
                            interval).replace("$D", byDayString); //$NON-NLS-1$
                }
            }


            String detail;
            if(task.getFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION))
                detail = context.getString(R.string.repeat_detail_completion, interval);
            else
                detail = context.getString(R.string.repeat_detail_duedate, interval);


            TaskDetail taskDetail = new TaskDetail(detail);

            // transmit
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        }

    }

}
