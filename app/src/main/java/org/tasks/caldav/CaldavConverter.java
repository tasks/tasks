package org.tasks.caldav;

import static com.todoroo.astrid.data.Task.URGENCY_SPECIFIC_DAY;
import static com.todoroo.astrid.data.Task.URGENCY_SPECIFIC_DAY_TIME;
import static org.tasks.date.DateTimeUtils.newDateTime;

import at.bitfire.ical4android.InvalidCalendarException;
import com.google.common.base.Strings;
import com.todoroo.astrid.data.Task;
import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.property.Completed;
import net.fortuna.ical4j.model.property.Due;
import net.fortuna.ical4j.model.property.RRule;
import org.tasks.data.CaldavTask;
import timber.log.Timber;

public class CaldavConverter {

  private static final DateFormat DUE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);

  public static void apply(Task local, at.bitfire.ical4android.Task remote) {
    Completed completedAt = remote.getCompletedAt();
    if (completedAt == null) {
      local.setCompletionDate(0L);
    } else {
      local.setCompletionDate(remote.getCompletedAt().getDate().getTime());
    }
    Long createdAt = remote.getCreatedAt();
    if (createdAt != null) {
      local.setCreationDate(newDateTime(createdAt).toLocal().getMillis());
    }
    local.setTitle(remote.getSummary());
    local.setNotes(remote.getDescription());
    local.setImportance(fromRemote(remote.getPriority()));
    RRule repeatRule = remote.getRRule();
    if (repeatRule == null) {
      local.setRecurrence("");
    } else {
      Recur recur = repeatRule.getRecur();
      if (recur.getInterval() <= 0) {
        recur.setInterval(1);
      }
      local.setRecurrence(
          "RRULE:" + recur.toString() + (local.repeatAfterCompletion() ? ";FROM=COMPLETION" : ""));
    }
    Due due = remote.getDue();
    if (due == null) {
      local.setDueDate(0L);
    } else {
      Date dueDate = due.getDate();
      if (dueDate instanceof DateTime) {
        local.setDueDate(Task.createDueDate(URGENCY_SPECIFIC_DAY_TIME, dueDate.getTime()));
      } else {
        try {
          local.setDueDate(
              Task.createDueDate(
                  URGENCY_SPECIFIC_DAY, DUE_DATE_FORMAT.parse(due.getValue()).getTime()));
        } catch (ParseException e) {
          Timber.e(e);
        }
      }
    }
  }

  private static int fromRemote(int remotePriority) {
    if (remotePriority == 0) {
      return Task.IMPORTANCE_NONE;
    }
    if (remotePriority == 5) {
      return Task.IMPORTANCE_MUST_DO;
    }
    return remotePriority < 5 ? Task.IMPORTANCE_DO_OR_DIE : Task.IMPORTANCE_SHOULD_DO;
  }

  private static int toRemote(int remotePriority, int localPriority) {
    if (localPriority == Task.IMPORTANCE_NONE) {
      return 0;
    }
    if (localPriority == Task.IMPORTANCE_MUST_DO) {
      return 5;
    }
    if (localPriority == Task.IMPORTANCE_DO_OR_DIE) {
      return remotePriority < 5 ? Math.max(1, remotePriority) : 1;
    }
    return remotePriority > 5 ? Math.min(9, remotePriority) : 9;
  }

  public static at.bitfire.ical4android.Task toCaldav(CaldavTask caldavTask, Task task) {
    at.bitfire.ical4android.Task remote = null;
    try {
      if (!Strings.isNullOrEmpty(caldavTask.getVtodo())) {
        remote =
            at.bitfire.ical4android.Task.fromReader(new StringReader(caldavTask.getVtodo())).get(0);
      }
    } catch (IOException | InvalidCalendarException e) {
      Timber.e(e);
    }
    if (remote == null) {
      remote = new at.bitfire.ical4android.Task();
    }
    remote.setCreatedAt(newDateTime(task.getCreationDate()).toUTC().getMillis());
    remote.setSummary(task.getTitle());
    remote.setDescription(task.getNotes());
    if (task.hasDueDate()) {
      if (task.hasDueTime()) {
        remote.setDue(new Due(new DateTime(task.getDueDate())));
      } else {
        try {
          remote.setDue(new Due(newDateTime(task.getDueDate()).toString("yyyyMMdd")));
        } catch (ParseException e) {
          Timber.e(e);
        }
      }
      remote.setDue(
          new Due(
              task.hasDueTime()
                  ? new DateTime(task.getDueDate())
                  : new Date(new org.tasks.time.DateTime(task.getDueDate()).toUTC().getMillis())));
    } else {
      remote.setDue(null);
    }
    remote.setCompletedAt(
        task.isCompleted() ? new Completed(new DateTime(task.getCompletionDate())) : null);
    if (task.isRecurring()) {
      try {
        String rrule = task.getRecurrenceWithoutFrom().replace("RRULE:", "");
        remote.setRRule(new RRule(rrule));
      } catch (ParseException e) {
        Timber.e(e);
      }
    } else {
      remote.setRRule(null);
    }
    remote.setLastModified(newDateTime(task.getModificationDate()).toUTC().getMillis());
    remote.setPriority(toRemote(remote.getPriority(), task.getImportance()));
    return remote;
  }
}
