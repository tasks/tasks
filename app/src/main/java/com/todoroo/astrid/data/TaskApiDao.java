/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;

import java.util.Collection;
import java.util.Set;

/**
 * Data access object for accessing Astrid's {@link Task} table. If you
 * are looking to store extended information about a Task, you probably
 * want to use the MetadataApiDao object.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskApiDao {

    /** @return true if task change shouldn't be broadcast */
    public static boolean insignificantChange(Collection<String> values) {
        if(values == null || values.size() == 0) {
            return true;
        }

        if(values.contains(Task.REMINDER_LAST.name) &&
                values.size() <= 2) {
            return true;
        }

        if(values.contains(Task.REMINDER_SNOOZE.name) &&
                values.size() <= 2) {
            return true;
        }

        if(values.contains(Task.TIMER_START.name) &&
                values.size() <= 2) {
            return true;
        }

        return false;
    }

}
