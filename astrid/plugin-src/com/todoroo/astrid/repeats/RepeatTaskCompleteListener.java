package com.todoroo.astrid.repeats;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.ical.iter.RecurrenceIterator;
import com.google.ical.iter.RecurrenceIteratorFactory;
import com.google.ical.values.DateTimeValueImpl;
import com.google.ical.values.DateValue;
import com.google.ical.values.DateValueImpl;
import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;

public class RepeatTaskCompleteListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        Task task = PluginServices.getTaskService().fetchById(taskId, Task.ID, Task.RECURRENCE,
                Task.DUE_DATE, Task.FLAGS, Task.HIDE_UNTIL);
        if(task == null)
            return;

        String recurrence = task.getValue(Task.RECURRENCE);
        if(recurrence != null && recurrence.length() > 0) {
            long newDueDate;
            try {
                newDueDate = computeNextDueDate(task, recurrence);
                if(newDueDate == -1)
                    return;
            } catch (ParseException e) {
                PluginServices.getExceptionService().reportError("repeat-parse", e); //$NON-NLS-1$
                return;
            }

            long hideUntil = task.getValue(Task.HIDE_UNTIL);
            if(hideUntil > 0 && task.getValue(Task.DUE_DATE) > 0) {
                hideUntil += newDueDate - task.getValue(Task.DUE_DATE);
            }

            // clone to create new task
            Task clone = PluginServices.getTaskService().clone(task);
            clone.setValue(Task.DUE_DATE, newDueDate);
            clone.setValue(Task.HIDE_UNTIL, hideUntil);
            clone.setValue(Task.COMPLETION_DATE, 0L);
            clone.setValue(Task.TIMER_START, 0L);
            clone.setValue(Task.ELAPSED_SECONDS, 0);
            PluginServices.getTaskService().save(clone);

            // clear recurrence from completed task so it can be re-completed
            task.setValue(Task.RECURRENCE, ""); //$NON-NLS-1$
            task.setValue(Task.DETAILS_DATE, 0L);
            PluginServices.getTaskService().save(task);

            // send a broadcast
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_REPEATED);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, clone.getId());
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_OLD_DUE_DATE, task.getValue(Task.DUE_DATE));
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_NEW_DUE_DATE, newDueDate);
            context.sendOrderedBroadcast(broadcastIntent, null);
        }
    }

    public static long computeNextDueDate(Task task, String recurrence) throws ParseException {
        DateValue repeatFrom;
        Date repeatFromDate = new Date();

        DateValue today = new DateValueImpl(repeatFromDate.getYear() + 1900,
                repeatFromDate.getMonth() + 1, repeatFromDate.getDate());
        boolean repeatAfterCompletion = task.getFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION);
        if(task.hasDueDate() && !repeatAfterCompletion) {
            repeatFromDate = new Date(task.getValue(Task.DUE_DATE));
            if(task.hasDueTime()) {
                repeatFrom = new DateTimeValueImpl(repeatFromDate.getYear() + 1900,
                        repeatFromDate.getMonth() + 1, repeatFromDate.getDate(),
                        repeatFromDate.getHours(), repeatFromDate.getMinutes(), repeatFromDate.getSeconds());
            } else {
                repeatFrom = today;
            }
        } else if (task.hasDueDate() && repeatAfterCompletion) {
            repeatFromDate = new Date(task.getValue(Task.DUE_DATE));
            if(task.hasDueTime()) {
                repeatFrom = new DateTimeValueImpl(today.year(),
                        today.month(), today.day(),
                        repeatFromDate.getHours(), repeatFromDate.getMinutes(), repeatFromDate.getSeconds());
            } else {
                repeatFrom = today;
            }
        } else {
            repeatFrom = today;
        }

        // invoke the recurrence iterator
        long newDueDate = -1;
        RRule rrule = new RRule(recurrence);

        // handle the iCalendar "byDay" field differently depending on if
        // we are weekly or otherwise

        if(rrule.getFreq() != Frequency.WEEKLY) {
            rrule.setByDay(Collections.EMPTY_LIST);
        }

        if(rrule.getFreq() == Frequency.HOURLY) {
            newDueDate = task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                    repeatFromDate.getTime() + DateUtilities.ONE_HOUR * rrule.getInterval());
        } else {
            RecurrenceIterator iterator = RecurrenceIteratorFactory.createRecurrenceIterator(rrule,
                    repeatFrom, TimeZone.getDefault());
            DateValue nextDate = repeatFrom;
            if(repeatFrom.compareTo(today) < 0)
                iterator.advanceTo(today);

            for(int i = 0; i < 10; i++) { // ten tries then we give up
                if(!iterator.hasNext())
                    return -1;
                nextDate = iterator.next();

                if(nextDate.compareTo(repeatFrom) == 0)
                    continue;

                if(nextDate instanceof DateTimeValueImpl) {
                    DateTimeValueImpl newDateTime = (DateTimeValueImpl)nextDate;
                    Date date = new Date(Date.UTC(newDateTime.year() - 1900, newDateTime.month() - 1,
                            newDateTime.day(), newDateTime.hour(),
                            newDateTime.minute(), newDateTime.second()));
                    // time may be inaccurate due to DST, force time to be same
                    date.setHours(repeatFromDate.getHours());
                    date.setMinutes(repeatFromDate.getMinutes());
                    newDueDate = task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                            date.getTime());
                } else {
                    newDueDate = task.createDueDate(Task.URGENCY_SPECIFIC_DAY,
                            new Date(nextDate.year() - 1900, nextDate.month() - 1,
                                    nextDate.day()).getTime());
                }

                if(newDueDate > DateUtilities.now() && (repeatAfterCompletion || (newDueDate != repeatFromDate.getTime())))
                    break;

            }
        }

        if(newDueDate == -1)
            return -1;

        return newDueDate;
    }

}
