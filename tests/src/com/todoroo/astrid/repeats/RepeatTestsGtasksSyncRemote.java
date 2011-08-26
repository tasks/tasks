package com.todoroo.astrid.repeats;

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
            com.google.api.services.tasks.v1.model.Task remoteModel, long dueDate) {
        long completionDate;
        if (completeBefore)
            completionDate = dueDate - DateUtilities.ONE_DAY;
        else
            completionDate = dueDate + DateUtilities.ONE_DAY;
        remoteModel.completed = GtasksApiUtilities.unixTimeToGtasksCompletionTime(completionDate);
        remoteModel.status = "completed";
        try {
            gtasksService.updateGtask(GtasksApiUtilities.extractListIdFromSelfLink(remoteModel), remoteModel);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception in gtasks service");
        }
        return completionDate;
    }
    @Override
    protected long computeNextDueDateFromDate(long fromDate, RRule rrule, boolean fromCompletion) {
        long expectedDate = super.computeNextDueDateFromDate(fromDate, rrule, fromCompletion);
        Frequency freq = rrule.getFreq();
        if (fromCompletion && (freq == Frequency.HOURLY || freq == Frequency.MINUTELY)) {
            long millis = (freq == Frequency.HOURLY ? DateUtilities.ONE_HOUR : DateUtilities.ONE_MINUTE);
            Date rounded = new Date(expectedDate);
            rounded.setHours(0);
            rounded.setMinutes(0);
            rounded.setSeconds(0);
            return rounded.getTime() + rrule.getInterval() * millis;
        } else {
            return expectedDate;
        }
    }
}
