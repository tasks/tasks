package com.todoroo.astrid.repeats;

import java.util.Date;

import org.json.JSONObject;

import com.google.ical.values.RRule;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

public class RepeatTestsProducteevSyncRemote extends RepeatTestsProducteevSync {

    @Override
    protected long setCompletionDate(boolean completeBefore, Task t,
            JSONObject remoteModel, long dueDate) {
        long completionDate;
        if (completeBefore)
            completionDate = dueDate - DateUtilities.ONE_DAY;
        else
            completionDate = dueDate + DateUtilities.ONE_DAY;

        try {
            invoker.tasksSetStatus(getRemoteId(remoteModel), 2);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error in producteev invoker");
        }
        return completionDate;
    }

    @Override
    protected long computeNextDueDateFromDate(long fromDate, RRule rrule, boolean fromCompletion) {
        if (fromCompletion) {
            fromDate = DateUtilities.now() - 5000L;
        }
        return super.computeNextDueDateFromDate(fromDate, rrule, fromCompletion);
    }

    @Override
    protected void assertTimesMatch(long expectedTime, long newDueDate) {
        assertTrue(String.format("Expected %s, was %s", new Date(expectedTime), new Date(newDueDate)),
                Math.abs(expectedTime - newDueDate) < 60000); // More lenient with producteev since we use now() in setting completion during sync
    }
}
