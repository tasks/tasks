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
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
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
                Task.DUE_DATE, Task.FLAGS);
        if(task == null)
            return;

        String recurrence = task.getValue(Task.RECURRENCE);
        if(recurrence.length() > 0) {
            Date date = new Date();
            DateValue today = new DateValueImpl(date.getYear() + 1900,
                    date.getMonth() + 1, date.getDate());

            DateValue repeatFrom;
            if(task.hasDueDate() && !task.getFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION)) {
                date = new Date(task.getValue(Task.DUE_DATE));
                repeatFrom = new DateTimeValueImpl(date.getYear() + 1900,
                        date.getMonth() + 1, date.getDate(),
                        date.getHours(), date.getMinutes(), date.getSeconds());
            } else {
                repeatFrom = today;
            }

            // invoke the recurrence iterator
            try {
                RecurrenceIterator iterator = RecurrenceIteratorFactory.createRecurrenceIterator(recurrence,
                        repeatFrom, TimeZone.getDefault());
                if(repeatFrom.compareTo(today) < 0)
                    repeatFrom = today;
                // go to the latest value and advance one more
                iterator.advanceTo(repeatFrom);
                if(!iterator.hasNext())
                    return;
                DateValue nextDate = iterator.next();
                if(nextDate.equals(repeatFrom)) {
                    if(!iterator.hasNext())
                        return;
                    nextDate = iterator.next();
                }

                long newDueDate;
                if(nextDate instanceof DateTimeValueImpl) {
                    DateTimeValueImpl newDateTime = (DateTimeValueImpl)nextDate;
                    newDueDate = task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                            new Date(newDateTime.year() - 1900, newDateTime.month() - 1,
                                    newDateTime.day(), newDateTime.hour(),
                                    newDateTime.minute(), newDateTime.second()).getTime());
                } else {
                    newDueDate = task.createDueDate(Task.URGENCY_SPECIFIC_DAY,
                            new Date(nextDate.year() - 1900, nextDate.month() - 1,
                                    nextDate.day()).getTime());
                }

                Task newTask = taskService.clone(task);
                task.setValue(Task.DUE_DATE, newDueDate);
                taskService.save(newTask, false);
            } catch (ParseException e) {
                exceptionService.reportError("recurrence-rule: " + recurrence, e); //$NON-NLS-1$
            }
        }
    }

}
