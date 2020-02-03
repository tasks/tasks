package org.tasks.caldav;

import static com.todoroo.andlib.utility.DateUtilities.now;
import static com.todoroo.astrid.data.Task.URGENCY_SPECIFIC_DAY;
import static com.todoroo.astrid.data.Task.URGENCY_SPECIFIC_DAY_TIME;
import static org.tasks.caldav.CaldavUtils.fromVtodo;
import static org.tasks.caldav.CaldavUtils.setParent;
import static org.tasks.date.DateTimeUtils.newDateTime;

import at.bitfire.ical4android.DateUtils;
import com.google.common.base.Strings;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Task.Priority;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.property.Completed;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Due;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Status;
import org.tasks.data.CaldavTask;
import timber.log.Timber;

public class CaldavConverter {

  private static final DateFormat DUE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);

  public static void apply(Task local, at.bitfire.ical4android.Task remote) {
    Completed completedAt = remote.getCompletedAt();
    if (completedAt != null) {
      local.setCompletionDate(remote.getCompletedAt().getDate().getTime());
    } else if (remote.getStatus() == Status.VTODO_COMPLETED) {
      if (!local.isCompleted()) {
        local.setCompletionDate(now());
      }
    } else {
      local.setCompletionDate(0L);
    }

    Long createdAt = remote.getCreatedAt();
    if (createdAt != null) {
      local.setCreationDate(newDateTime(createdAt).toLocal().getMillis());
    }
    local.setTitle(remote.getSummary());
    local.setNotes(remote.getDescription());
    local.setPriority(fromRemote(remote.getPriority()));
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
        local.setDueDateAdjustingHideUntil(
            Task.createDueDate(URGENCY_SPECIFIC_DAY_TIME, dueDate.getTime()));
      } else {
        try {
          local.setDueDateAdjustingHideUntil(
              Task.createDueDate(
                  URGENCY_SPECIFIC_DAY, DUE_DATE_FORMAT.parse(due.getValue()).getTime()));
        } catch (ParseException e) {
          Timber.e(e);
        }
      }
    }
  }

  private static @Priority int fromRemote(int remotePriority) {
    if (remotePriority == 0) {
      return Priority.NONE;
    }
    if (remotePriority == 5) {
      return Priority.MEDIUM;
    }
    return remotePriority < 5 ? Priority.HIGH : Priority.LOW;
  }

  private static int toRemote(int remotePriority, int localPriority) {
    if (localPriority == Priority.NONE) {
      return 0;
    }
    if (localPriority == Priority.MEDIUM) {
      return 5;
    }
    if (localPriority == Priority.HIGH) {
      return remotePriority < 5 ? Math.max(1, remotePriority) : 1;
    }
    return remotePriority > 5 ? Math.min(9, remotePriority) : 9;
  }

  public static at.bitfire.ical4android.Task toCaldav(CaldavTask caldavTask, Task task) {
    at.bitfire.ical4android.Task remote = null;
    try {
      if (!Strings.isNullOrEmpty(caldavTask.getVtodo())) {
        remote = fromVtodo(caldavTask.getVtodo());
      }
    } catch (Exception e) {
      Timber.e(e);
    }
    if (remote == null) {
      remote = new at.bitfire.ical4android.Task();
    }
    remote.setCreatedAt(newDateTime(task.getCreationDate()).toUTC().getMillis());
    remote.setSummary(task.getTitle());
    remote.setDescription(task.getNotes());
    if (task.hasDueTime()) {
      net.fortuna.ical4j.model.TimeZone tz =
          DateUtils.INSTANCE.getTzRegistry().getTimeZone(TimeZone.getDefault().getID());
      DateTime dateTime = new DateTime(tz != null
          ? task.getDueDate()
          : new org.tasks.time.DateTime(task.getDueDate()).toUTC().getMillis());
      dateTime.setTimeZone(tz);
      remote.setDue(new Due(dateTime));
    } else if (task.hasDueDate()) {
      remote.setDue(new Due(new Date(task.getDueDate())));
    } else {
      remote.setDue(null);
    }
    if (task.isCompleted()) {
      remote.setCompletedAt(new Completed(new DateTime(task.getCompletionDate())));
      remote.setStatus(Status.VTODO_COMPLETED);
      remote.setPercentComplete(100);
    } else if (remote.getCompletedAt() != null) {
      remote.setCompletedAt(null);
      remote.setStatus(null);
      remote.setPercentComplete(null);
    }
    if (task.isRecurring()) {
      try {
        String rrule = task.getRecurrenceWithoutFrom().replace("RRULE:", "");
        remote.setRRule(new RRule(rrule));
        if (remote.getDtStart() == null) {
          Date date = remote.getDue() != null ? remote.getDue().getDate() : new Date();
          remote.setDtStart(new DtStart(date));
        }
      } catch (ParseException e) {
        Timber.e(e);
      }
    } else {
      remote.setRRule(null);
    }
    remote.setLastModified(newDateTime(task.getModificationDate()).toUTC().getMillis());
    remote.setPriority(toRemote(remote.getPriority(), task.getPriority()));
    setParent(remote, task.getParent() == 0 ? null : caldavTask.getRemoteParent());

    return remote;
  }
}
