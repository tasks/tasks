package org.tasks.makers;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import com.natpryce.makeiteasy.PropertyValue;
import com.todoroo.astrid.data.Task;

import org.tasks.time.DateTime;

import static com.natpryce.makeiteasy.Property.newProperty;
import static org.tasks.makers.Maker.make;

public class TaskMaker {

    public static Property<Task, DateTime> DUE_DATE = newProperty();
    public static Property<Task, DateTime> DUE_TIME = newProperty();
    public static Property<Task, DateTime> REMINDER_LAST = newProperty();
    public static Property<Task, Integer> HIDE_TYPE = newProperty();
    public static Property<Task, Integer> REMINDERS = newProperty();

    @SafeVarargs
    public static Task newTask(PropertyValue<? super Task, ?>... properties) {
        return make(instantiator, properties);
    }

    private static final Instantiator<Task> instantiator = new Instantiator<Task>() {
        @Override
        public Task instantiate(PropertyLookup<Task> lookup) {
            Task task = new Task();

            DateTime dueDate = lookup.valueOf(DUE_DATE, (DateTime) null);
            if (dueDate != null) {
                task.setDueDate(Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dueDate.getMillis()));
            }

            DateTime dueTime = lookup.valueOf(DUE_TIME, (DateTime) null);
            if (dueTime != null) {
                task.setDueDate(Task.createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, dueTime.getMillis()));
            }

            int hideType = lookup.valueOf(HIDE_TYPE, -1);
            if (hideType >= 0) {
                task.setHideUntil(task.createHideUntil(hideType, 0));
            }

            int reminderFlags = lookup.valueOf(REMINDERS, -1);
            if (reminderFlags >= 0) {
                task.setReminderFlags(reminderFlags);
            }

            DateTime reminderLast = lookup.valueOf(REMINDER_LAST, (DateTime) null);
            if (reminderLast != null) {
                task.setReminderLast(reminderLast.getMillis());
            }

            return task;
        }
    };
}
