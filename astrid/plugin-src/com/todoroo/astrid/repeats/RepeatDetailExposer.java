/**
 * Copyright (c) 2012 Todoroo Inc
 *
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
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

/**
 * Exposes Task Detail for repeats, i.e. "Repeats every 2 days"
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class RepeatDetailExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // get tags associated with this task
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        String taskDetail = getTaskDetails(context, taskId);
        if(taskDetail == null)
            return;

        // transmit
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_DETAILS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, RepeatsPlugin.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, taskDetail);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    public String getTaskDetails(Context context, long id) {
        Task task = PluginServices.getTaskService().fetchById(id, Task.RECURRENCE);
        if(task == null)
            return null;

        Resources r = context.getResources();

        String recurrence = task.sanitizedRecurrence();
        if(recurrence != null && recurrence.length() > 0) {
            RRule rrule;
            try {
                rrule = new RRule(recurrence);
            } catch (ParseException e) {
                System.err.println(e.toString());
                return null;
            }

            String interval = getIntervalFor(r, rrule);

            interval = "<b>" + interval + "</b>";  //$NON-NLS-1$//$NON-NLS-2$
            List<WeekdayNum> byDay = rrule.getByDay();
            if(rrule.getFreq() == Frequency.WEEKLY || byDay.size() != 7) {
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
            if(task.repeatAfterCompletion())
                detail = context.getString(R.string.repeat_detail_completion, interval);
            else
                detail = context.getString(R.string.repeat_detail_duedate, interval);

            return "<img src='silk_date'/> " + detail; //$NON-NLS-1$
        }
        return null;
    }

    private String getIntervalFor(Resources r, RRule rrule) {
        int plural;
        switch(rrule.getFreq()) {
        case MINUTELY:
            plural = R.plurals.DUt_minutes; break;
        case HOURLY:
            plural = R.plurals.DUt_hours; break;
        case DAILY:
            plural = R.plurals.DUt_days; break;
        case WEEKLY:
            plural = R.plurals.DUt_weeks; break;
        case MONTHLY:
            plural = R.plurals.DUt_months; break;
        case YEARLY:
            plural = R.plurals.DUt_years; break;
        default:
            // not designed to be used, only a fail-safe
            return rrule.getInterval() + "-" + rrule.getFreq().name(); //$NON-NLS-1$
        }

        return r.getQuantityString(plural, rrule.getInterval(), rrule.getInterval());
    }

    public String getPluginIdentifier() {
        return RepeatsPlugin.IDENTIFIER;
    }

}
