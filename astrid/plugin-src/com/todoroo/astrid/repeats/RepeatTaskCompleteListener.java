package com.todoroo.astrid.repeats;

import java.text.ParseException;
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
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.TaskService;

public class RepeatTaskCompleteListener extends BroadcastReceiver {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ExceptionService exceptionService;

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        DependencyInjectionService.getInstance().inject(this);

        Task task = taskService.fetchById(taskId, Task.ID, Task.RECURRENCE,
                Task.DUE_DATE, Task.FLAGS, Task.HIDE_UNTIL);
        if(task == null)
            return;

        String recurrence = task.getValue(Task.RECURRENCE);
        if(recurrence != null && recurrence.length() > 0) {
            DateValue repeatFrom;
            Date repeatFromDate = new Date();

            DateValue today = new DateValueImpl(repeatFromDate.getYear() + 1900,
                    repeatFromDate.getMonth() + 1, repeatFromDate.getDate());
            if(task.hasDueDate() && !task.getFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION)) {
                repeatFromDate = new Date(task.getValue(Task.DUE_DATE));
                if(task.hasDueTime()) {
                    repeatFrom = new DateTimeValueImpl(repeatFromDate.getYear() + 1900,
                            repeatFromDate.getMonth() + 1, repeatFromDate.getDate(),
                            repeatFromDate.getHours(), repeatFromDate.getMinutes(), repeatFromDate.getSeconds());
                } else {
                    repeatFrom = new DateValueImpl(repeatFromDate.getYear() + 1900,
                            repeatFromDate.getMonth() + 1, repeatFromDate.getDate());
                }
            } else {
                repeatFrom = today;
            }

            // invoke the recurrence iterator
            try {
                long newDueDate;
                RRule rrule = new RRule(recurrence);
                if(rrule.getFreq() == Frequency.HOURLY) {
                    newDueDate = task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                            repeatFromDate.getTime() + DateUtilities.ONE_HOUR * rrule.getInterval());
                } else {
                    RecurrenceIterator iterator = RecurrenceIteratorFactory.createRecurrenceIterator(rrule,
                            repeatFrom, TimeZone.getDefault());
                    DateValue nextDate;
                    if(repeatFrom.compareTo(today) < 0) {
                        iterator.advanceTo(today);
                        if(!iterator.hasNext())
                            return;
                        nextDate = iterator.next();
                    } else {
                        iterator.advanceTo(repeatFrom);
                        if(!iterator.hasNext())
                            return;
                        nextDate = iterator.next();
                        nextDate = iterator.next();
                    }

                    if(nextDate instanceof DateTimeValueImpl) {
                        DateTimeValueImpl newDateTime = (DateTimeValueImpl)nextDate;
                        newDueDate = task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                                Date.UTC(newDateTime.year() - 1900, newDateTime.month() - 1,
                                        newDateTime.day(), newDateTime.hour(),
                                        newDateTime.minute(), newDateTime.second()));
                    } else {
                        newDueDate = task.createDueDate(Task.URGENCY_SPECIFIC_DAY,
                                new Date(nextDate.year() - 1900, nextDate.month() - 1,
                                        nextDate.day()).getTime());
                    }
                }

                long hideUntil = task.getValue(Task.HIDE_UNTIL);
                if(hideUntil > 0 && task.getValue(Task.DUE_DATE) > 0) {
                    hideUntil += newDueDate - task.getValue(Task.DUE_DATE);
                }

                task = taskService.clone(task);
                task.setValue(Task.DUE_DATE, newDueDate);
                task.setValue(Task.HIDE_UNTIL, hideUntil);
                task.setValue(Task.COMPLETION_DATE, 0L);
                taskService.save(task, false);
            } catch (ParseException e) {
                exceptionService.reportError("recurrence-rule: " + recurrence, e); //$NON-NLS-1$
            }
        }
    }

}
