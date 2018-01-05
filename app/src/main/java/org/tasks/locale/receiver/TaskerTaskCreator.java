package org.tasks.locale.receiver;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.TaskCreator;

import org.tasks.locale.bundle.TaskCreationBundle;
import org.tasks.time.DateTime;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.format.DateTimeFormatter;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

public class TaskerTaskCreator {

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;

    private final TaskCreator taskCreator;
    private final TaskDao taskDao;

    @Inject
    public TaskerTaskCreator(TaskCreator taskCreator, TaskDao taskDao) {
        this.taskCreator = taskCreator;
        this.taskDao = taskDao;
    }

    public void handle(TaskCreationBundle bundle) {
        Task task = taskCreator.createWithValues(null, bundle.getTitle());

        String dueDateString = bundle.getDueDate();
        if (!isNullOrEmpty(dueDateString)) {
            try {
                LocalDate dueDate = LocalDate.parse(dueDateString, dateFormatter);
                DateTime dt = new DateTime(dueDate.getYear(), dueDate.getMonthValue(), dueDate.getDayOfMonth());
                task.setDueDate(Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dt.getMillis()));
            } catch(Exception e) {
                Timber.e(e, e.getMessage());
            }
        }

        String dueTimeString = bundle.getDueTime();
        if (!isNullOrEmpty(dueTimeString)) {
            try {
                LocalTime dueTime = LocalTime.parse(dueTimeString, timeFormatter);
                task.setDueDate(Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME,
                        new DateTime(task.hasDueDate() ? task.getDueDate() : currentTimeMillis())
                                .withHourOfDay(dueTime.getHour())
                                .withMinuteOfHour(dueTime.getMinute())
                                .getMillis()));
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }
        }

        String priorityString = bundle.getPriority();
        if (!isNullOrEmpty(priorityString)) {
            try {
                int priority = Integer.parseInt(priorityString);
                task.setImportance(Math.max(0, Math.min(3, priority)));
            } catch (NumberFormatException e) {
                Timber.e(e, e.getMessage());
            }
        }

        task.setNotes(bundle.getDescription());

        taskDao.save(task);
    }
}
