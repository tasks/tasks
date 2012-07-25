/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync.repeats;

import java.io.IOException;
import java.util.Date;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.api.GtasksApiUtilities;

public class RepeatTestsGtasksSyncRemote extends RepeatTestsGtasksSync {

    // Test logic in superclass

    @Override
    protected long setCompletionDate(boolean completeBefore, Task t,
            com.google.api.services.tasks.model.Task remoteModel, long dueDate) {
        long completionDate;
        if (completeBefore)
            completionDate = dueDate - DateUtilities.ONE_DAY;
        else
            completionDate = dueDate + DateUtilities.ONE_DAY;
        remoteModel.setCompleted(GtasksApiUtilities.unixTimeToGtasksCompletionTime(completionDate));
        remoteModel.setStatus("completed");
        try {
            gtasksService.updateGtask(DEFAULT_LIST, remoteModel);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception in gtasks service");
        }
        return completionDate;
    }
}
