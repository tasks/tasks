/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync.repeats;

import java.util.ArrayList;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

public class RepeatTestsActFmSyncRemote extends RepeatTestsActFmSync {
    @Override
    protected long setCompletionDate(boolean completeBefore, Task t,
            Task remoteModel, long dueDate) {
        long completionDate = DateUtilities.now();

        ArrayList<Object> params = new ArrayList<Object>();
        params.add("completed"); params.add(completionDate / 1000L);

        params.add("id"); params.add(remoteModel.getValue(Task.REMOTE_ID));
        try {
            invoker.invoke("task_save", params.toArray(new Object[params.size()]));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error in actfm invoker");
        }
        return completionDate;
    }

}
