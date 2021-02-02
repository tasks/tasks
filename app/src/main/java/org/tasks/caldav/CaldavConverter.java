package org.tasks.caldav;

import static com.todoroo.andlib.utility.DateUtilities.now;
import static com.todoroo.astrid.data.Task.withoutRRULE;
import static org.tasks.caldav.iCalendar.getLocal;
import static org.tasks.date.DateTimeUtils.newDateTime;
import static org.tasks.time.DateTime.UTC;
import static org.tasks.time.DateTimeUtils.startOfDay;

import at.bitfire.ical4android.DateUtils;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Task.Priority;
import java.text.ParseException;
import java.util.TimeZone;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.Completed;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Due;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Status;
import org.tasks.data.CaldavTask;
import timber.log.Timber;

public class CaldavConverter {

  public static void apply(Task local, at.bitfire.ical4android.Task remote) {
    Completed completedAt = remote.getCompletedAt();
    if (completedAt != null) {
      local.setCompletionDate(getLocal(completedAt));
    } else if (remote.getStatus() == Status.VTODO_COMPLETED) {
      if (!local.isCompleted()) {
        local.setCompletionDate(now());
      }
    } else {
      local.setCompletionDate(0L);
    }
    Long createdAt = remote.getCreatedAt();
    if (createdAt != null) {
      local.setCreationDate(newDateTime(createdAt, UTC).toLocal().getMillis());
    }
    local.setTitle(remote.getSummary());
    local.setNotes(remote.getDescription());
    local.setPriority(fromRemote(remote.getPriority()));
    local.setRecurrence(remote.getRRule());
    iCalendar.Companion.apply(remote.getDue(), local);
    iCalendar.Companion.apply(remote.getDtStart(), local);
  }

  public static @Priority int fromRemote(int remotePriority) {
    // https://tools.ietf.org/html/rfc5545#section-3.8.1.9
    if (remotePriority == 0) {
      return Priority.NONE;
    }
    if (remotePriority == 5) {
      return Priority.MEDIUM;
    }
    return remotePriority < 5 ? Priority.HIGH : Priority.LOW;
  }

  public static int toRemote(int remotePriority, int localPriority) {
    switch (localPriority) {
      case Priority.NONE:
        return 0;

      case Priority.MEDIUM:
        return 5;

      case Priority.HIGH:
        return remotePriority < 5 ? Math.max(1, remotePriority) : 1;

      default:
        return remotePriority > 5 ? Math.min(9, remotePriority) : 9;
    }
  }

  public static void toCaldav(CaldavTask caldavTask, Task task, at.bitfire.ical4android.Task remote) {
    remote.setCreatedAt(newDateTime(task.getCreationDate()).toUTC().getMillis());
    remote.setSummary(task.getTitle());
    remote.setDescription(task.getNotes());
    boolean allDay = !task.hasDueTime() && !task.hasStartTime();
    long dueDate = task.hasDueTime() ? task.getDueDate() : startOfDay(task.getDueDate());
    long startDate = task.hasStartTime() ? task.getHideUntil() : startOfDay(task.getHideUntil());
    if (dueDate > 0) {
      startDate = Math.min(dueDate, startDate);
      remote.setDue(new Due(allDay ? getDate(dueDate) : getDateTime(dueDate)));
    } else {
      remote.setDue(null);
    }
    if (startDate > 0) {
      remote.setDtStart(new DtStart(allDay ? getDate(startDate) : getDateTime(startDate)));
    } else {
      remote.setDtStart(null);
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
        RRule rrule = new RRule(withoutRRULE(task.getRecurrenceWithoutFrom()));
        long repeatUntil = task.getRepeatUntil();
        rrule
            .getRecur()
            .setUntil(
                repeatUntil > 0 ? new DateTime(newDateTime(repeatUntil).toUTC().getMillis()) : null);
        String sanitized = Task.sanitizeRRule(rrule.getValue()); // ical4j adds COUNT=-1 if there is an UNTIL value
        remote.setRRule(new RRule(sanitized));
      } catch (ParseException e) {
        Timber.e(e);
      }
    } else {
      remote.setRRule(null);
    }
    remote.setLastModified(newDateTime(task.getModificationDate()).toUTC().getMillis());
    remote.setPriority(toRemote(remote.getPriority(), task.getPriority()));
    iCalendar.Companion.setParent(remote, task.getParent() == 0 ? null : caldavTask.getRemoteParent());
  }

  private static Date getDate(long timestamp) {
    return new Date(timestamp + newDateTime(timestamp).getOffset());
  }

  private static DateTime getDateTime(long timestamp) {
    net.fortuna.ical4j.model.TimeZone tz =
        DateUtils.INSTANCE.ical4jTimeZone(TimeZone.getDefault().getID());
    DateTime dateTime = new DateTime(tz != null
        ? timestamp
        : new org.tasks.time.DateTime(timestamp).toUTC().getMillis());
    dateTime.setTimeZone(tz);
    return dateTime;
  }
}
