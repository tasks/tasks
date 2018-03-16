package org.tasks.caldav;

import com.google.common.base.Strings;
import com.todoroo.astrid.data.Task;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.property.Completed;
import net.fortuna.ical4j.model.property.Due;
import net.fortuna.ical4j.model.property.RRule;

import org.tasks.data.CaldavTask;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import at.bitfire.ical4android.InvalidCalendarException;
import timber.log.Timber;

import static com.todoroo.astrid.data.Task.URGENCY_SPECIFIC_DAY;
import static com.todoroo.astrid.data.Task.URGENCY_SPECIFIC_DAY_TIME;
import static org.tasks.date.DateTimeUtils.newDateTime;

public class TaskConverter {

    private static DateFormat DUE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);

    public static void apply(Task local, at.bitfire.ical4android.Task remote) {
        if (remote.getCompletedAt() != null) {
            local.setCompletionDate(remote.getCompletedAt().getDate().getTime());
        }
        local.setTitle(remote.getSummary());
        local.setNotes(remote.getDescription());
        local.setImportance(fromRemote(remote.getPriority()));
        RRule repeatRule = remote.getRRule();
        if (repeatRule != null) {
            Recur recur = repeatRule.getRecur();
            if (recur.getInterval() == 0) {
                recur.setInterval(1);
            }
            local.setRecurrence("RRULE:" + recur.toString() + (local.repeatAfterCompletion() ? ";FROM=COMPLETION" : ""));
        }
        Due due = remote.getDue();
        if (due != null) {
            Date dueDate = due.getDate();
            if (dueDate instanceof DateTime) {
                local.setDueDate(Task.createDueDate(URGENCY_SPECIFIC_DAY_TIME, dueDate.getTime()));
            } else {
                try {
                    local.setDueDate(Task.createDueDate(URGENCY_SPECIFIC_DAY, DUE_DATE_FORMAT.parse(due.getValue()).getTime()));
                } catch (ParseException e) {
                    Timber.e(e, e.getMessage());
                }
            }
        }
    }

    static int fromRemote(int remotePriority) {
        switch (remotePriority) {
            case 0:
                return Task.IMPORTANCE_NONE;
            case 1:
                return Task.IMPORTANCE_DO_OR_DIE;
            case 2:
                return Task.IMPORTANCE_MUST_DO;
            default:
                return Task.IMPORTANCE_SHOULD_DO;
        }
    }

    static int toRemote(int remotePriority, int localPriority) {
        switch (localPriority) {
            case Task.IMPORTANCE_DO_OR_DIE:
                return 1;
            case Task.IMPORTANCE_MUST_DO:
                return 2;
            case Task.IMPORTANCE_SHOULD_DO:
                return remotePriority > 2 ? remotePriority : 3;
            default:
                return 0;
        }
    }

    public static at.bitfire.ical4android.Task toCaldav(CaldavTask caldavTask, Task task) {
        at.bitfire.ical4android.Task remote = null;
        try {
            if (!Strings.isNullOrEmpty(caldavTask.getVtodo())) {
                remote = at.bitfire.ical4android.Task.fromReader(new StringReader(caldavTask.getVtodo())).get(0);
            }
        } catch (IOException | InvalidCalendarException e) {
            Timber.e(e, e.getMessage());
        }
        if (remote == null) {
            remote = new at.bitfire.ical4android.Task();
        }
        remote.setSummary(task.getTitle());
        remote.setDescription(task.getNotes());
        if (task.hasDueDate()) {
            if (task.hasDueTime()) {
                remote.setDue(new Due(new DateTime(task.getDueDate())));
            } else {
                try {
                    remote.setDue(new Due(newDateTime(task.getDueDate()).toString("yyyyMMdd")));
                } catch (ParseException e) {
                    Timber.e(e, e.getMessage());
                }
            }
            remote.setDue(new Due(task.hasDueTime()
                    ? new DateTime(task.getDueDate())
                    : new Date(new org.tasks.time.DateTime(task.getDueDate()).toUTC().getMillis())));
        }
        if (task.isCompleted()) {
            remote.setCompletedAt(new Completed(new DateTime(task.getCompletionDate())));
        }
        if (task.isRecurring()) {
            try {
                String rrule = task
                        .getRecurrenceWithoutFrom()
                        .replace("RRULE:", "");
                remote.setRRule(new RRule(rrule));
            } catch (ParseException e) {
                Timber.e(e, e.getMessage());
            }
        }
        remote.setLastModified(newDateTime(task.getModificationDate()).toUTC().getMillis());
        remote.setPriority(toRemote(remote.getPriority(), task.getImportance()));
        return remote;
    }
}
