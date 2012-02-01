package com.todoroo.astrid.repeats;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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
import com.google.ical.values.WeekdayNum;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.utility.Flags;

public class RepeatTaskCompleteListener extends BroadcastReceiver {

    @Autowired ActFmPreferenceService actFmPreferenceService;

    private static boolean skipActFmCheck = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        DependencyInjectionService.getInstance().inject(this);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        Task task = PluginServices.getTaskService().fetchById(taskId, Task.PROPERTIES);
        if(task == null || !task.isCompleted())
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

            StatisticsService.reportEvent(StatisticsConstants.V2_TASK_REPEAT);

            long hideUntil = task.getValue(Task.HIDE_UNTIL);
            if(hideUntil > 0 && task.getValue(Task.DUE_DATE) > 0) {
                hideUntil += newDueDate - task.getValue(Task.DUE_DATE);
            }

            // update repeat time when it repeats on the server
            long oldDueDate = task.getValue(Task.DUE_DATE);
            task.setValue(Task.COMPLETION_DATE, 0L);
            task.setValue(Task.DUE_DATE, newDueDate);
            task.setValue(Task.HIDE_UNTIL, hideUntil);
            task.putTransitory("repeat-complete", true); //$NON-NLS-1$
            GCalHelper.createTaskEventIfEnabled(task, false);
            PluginServices.getTaskService().save(task);

            // send a broadcast
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_EVENT_TASK_REPEATED);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, task.getId());
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_OLD_DUE_DATE, oldDueDate);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_NEW_DUE_DATE, newDueDate);
            context.sendOrderedBroadcast(broadcastIntent, null);
            Flags.set(Flags.REFRESH);
            return;

//            // clone to create new task
//            Flags.set(Flags.ACTFM_SUPPRESS_SYNC);
//            Flags.set(Flags.GTASKS_SUPPRESS_SYNC);
//            Task clone = PluginServices.getTaskService().clone(task);
//            clone.setValue(Task.DUE_DATE, newDueDate);
//            clone.setValue(Task.HIDE_UNTIL, hideUntil);
//            clone.setValue(Task.COMPLETION_DATE, 0L);
//            clone.setValue(Task.TIMER_START, 0L);
//            clone.setValue(Task.ELAPSED_SECONDS, 0);
//            clone.setValue(Task.REMINDER_SNOOZE, 0L);
//            clone.setValue(Task.REMINDER_LAST, 0L);
//            clone.setValue(Task.CALENDAR_URI, ""); //$NON-NLS-1$
//
//            GCalHelper.createTaskEventIfEnabled(clone, false);
//            PluginServices.getTaskService().save(clone);
//
//            // clear recurrence from completed task so it can be re-completed
//            task.setValue(Task.RECURRENCE, ""); //$NON-NLS-1$
//            task.setValue(Task.DETAILS_DATE, 0L);
//
//            PluginServices.getTaskService().save(task);

        }
    }

    /** for debug */
    public static void setSkipActFmCheck(boolean skipActFmCheck) {
        RepeatTaskCompleteListener.skipActFmCheck = skipActFmCheck;
    }

    /** Compute next due date */
    public static long computeNextDueDate(Task task, String recurrence) throws ParseException {
        boolean repeatAfterCompletion = task.getFlag(Task.FLAGS, Task.FLAG_REPEAT_AFTER_COMPLETION);
        RRule rrule = initRRule(recurrence);

        // initialize startDateAsDV
        Date original = setUpStartDate(task, repeatAfterCompletion, rrule.getFreq());
        DateValue startDateAsDV = setUpStartDateAsDV(task, original);

        if(rrule.getFreq() == Frequency.HOURLY || rrule.getFreq() == Frequency.MINUTELY)
            return handleSubdayRepeat(original, rrule);
        else if(rrule.getFreq() == Frequency.WEEKLY && rrule.getByDay().size() > 0 && repeatAfterCompletion)
            return handleWeeklyRepeatAfterComplete(rrule, original, task.hasDueTime());
        else
            return invokeRecurrence(rrule, original, startDateAsDV);
    }

    private static long handleWeeklyRepeatAfterComplete(RRule rrule, Date original,
            boolean hasDueTime) {
        List<WeekdayNum> byDay = rrule.getByDay();
        long newDate = original.getTime();
        newDate += DateUtilities.ONE_WEEK * (rrule.getInterval() - 1);
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(newDate);

        Collections.sort(byDay, weekdayCompare);
        WeekdayNum next = findNextWeekday(byDay, date);

        do {
            date.add(Calendar.DATE, 1);
        } while (date.get(Calendar.DAY_OF_WEEK) != next.wday.javaDayNum);

        long time = date.getTimeInMillis();
        if(hasDueTime)
            return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, time);
        else
            return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, time);
    }

    private static Comparator<WeekdayNum> weekdayCompare = new Comparator<WeekdayNum>() {
        @Override
        public int compare(WeekdayNum object1, WeekdayNum object2) {
            return object1.wday.javaDayNum - object2.wday.javaDayNum;
        }

    };

    private static WeekdayNum findNextWeekday(List<WeekdayNum> byDay,
            Calendar date) {
        WeekdayNum next = byDay.get(0);
        for (int i = 0; i < byDay.size(); i++) {
            WeekdayNum weekday = byDay.get(i);
            if (weekday.wday.javaDayNum > date.get(Calendar.DAY_OF_WEEK)) {
                return weekday;
            }
        }
        return next;
    }

    private static long invokeRecurrence(RRule rrule, Date original,
            DateValue startDateAsDV) {
        long newDueDate = -1;
        RecurrenceIterator iterator = RecurrenceIteratorFactory.createRecurrenceIterator(rrule,
                startDateAsDV, TimeZone.getDefault());
        DateValue nextDate = startDateAsDV;

        for(int i = 0; i < 10; i++) { // ten tries then we give up
            if(!iterator.hasNext())
                return -1;
            nextDate = iterator.next();

            if(nextDate.compareTo(startDateAsDV) == 0)
                continue;

            newDueDate = buildNewDueDate(original, nextDate);

            // detect if we finished
            if(newDueDate > original.getTime())
                break;
        }
        return newDueDate;
    }

    /** Compute long due date from DateValue */
    private static long buildNewDueDate(Date original, DateValue nextDate) {
        long newDueDate;
        if(nextDate instanceof DateTimeValueImpl) {
            DateTimeValueImpl newDateTime = (DateTimeValueImpl)nextDate;
            Date date = new Date(Date.UTC(newDateTime.year() - 1900, newDateTime.month() - 1,
                    newDateTime.day(), newDateTime.hour(),
                    newDateTime.minute(), newDateTime.second()));
            // time may be inaccurate due to DST, force time to be same
            date.setHours(original.getHours());
            date.setMinutes(original.getMinutes());
            newDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                    date.getTime());
        } else {
            newDueDate = Task.createDueDate(Task.URGENCY_SPECIFIC_DAY,
                    new Date(nextDate.year() - 1900, nextDate.month() - 1,
                            nextDate.day()).getTime());
        }
        return newDueDate;
    }

    /** Initialize RRule instance */
    private static RRule initRRule(String recurrence) throws ParseException {
        RRule rrule = new RRule(recurrence);

        // handle the iCalendar "byDay" field differently depending on if
        // we are weekly or otherwise
        if(rrule.getFreq() != Frequency.WEEKLY)
            rrule.setByDay(Collections.EMPTY_LIST);

        return rrule;
    }

    /** Set up repeat start date
     * @param frequency */
    private static Date setUpStartDate(Task task, boolean repeatAfterCompletion, Frequency frequency) {
        Date startDate = new Date();
        if(task.hasDueDate()) {
            Date dueDate = new Date(task.getValue(Task.DUE_DATE));
            if(repeatAfterCompletion)
                startDate = new Date(task.getValue(Task.COMPLETION_DATE));
            else
                startDate = dueDate;

            if(task.hasDueTime() && frequency != Frequency.HOURLY && frequency != Frequency.MINUTELY) {
                startDate.setHours(dueDate.getHours());
                startDate.setMinutes(dueDate.getMinutes());
                startDate.setSeconds(dueDate.getSeconds());
            }
        }
        return startDate;
    }

    private static DateValue setUpStartDateAsDV(Task task, Date startDate) {
        if(task.hasDueTime())
            return new DateTimeValueImpl(startDate.getYear() + 1900,
                    startDate.getMonth() + 1, startDate.getDate(),
                    startDate.getHours(), startDate.getMinutes(), startDate.getSeconds());
        else
            return new DateValueImpl(startDate.getYear() + 1900,
                    startDate.getMonth() + 1, startDate.getDate());
    }

    private static long handleSubdayRepeat(Date startDate, RRule rrule) {
        long millis;
        switch(rrule.getFreq()) {
        case HOURLY:
            millis = DateUtilities.ONE_HOUR;
            break;
        case MINUTELY:
            millis = DateUtilities.ONE_MINUTE;
            break;
        default:
            throw new RuntimeException("Error handing subday repeat: " + rrule.getFreq()); //$NON-NLS-1$
        }
        long newDueDate = startDate.getTime() + millis * rrule.getInterval();
        return Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                newDueDate);
    }

}
