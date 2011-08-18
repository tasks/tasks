package com.todoroo.astrid.repeats;

import java.util.ArrayList;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;

public class RepeatTestsActFmSyncRemote extends RepeatTestsActFmSync {
    @Override
    protected long setCompletionDate(boolean completeBefore, Task t,
            Task remoteModel, long dueDate) {
        long completionDate;
        if (completeBefore)
            completionDate = dueDate - DateUtilities.ONE_DAY;
        else
            completionDate = dueDate + DateUtilities.ONE_DAY;

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

    @Override
    public void testRepeatMinutelyFromCompleteDateCompleteBefore() {
        // (DISABLED) super.testRepeatMinutelyFromCompleteDateCompleteBefore();
    }

    @Override
    public void testRepeatMinutelyFromCompleteDateCompleteAfter() {
        // (DISABLED) super.testRepeatMinutelyFromCompleteDateCompleteAfter();
    }

    @Override
    public void testRepeatHourlyFromCompleteDateCompleteBefore() {
        // (DISABLED) super.testRepeatHourlyFromCompleteDateCompleteBefore();
    }

    @Override
    public void testRepeatHourlyFromCompleteDateCompleteAfter() {
        // (DISABLED) super.testRepeatHourlyFromCompleteDateCompleteAfter();
    }

    @Override
    public void testRepeatDailyFromCompleteDateCompleteBefore() {
        // (DISABLED) super.testRepeatDailyFromCompleteDateCompleteBefore();
    }

    @Override
    public void testRepeatDailyFromCompleteDateCompleteAfter() {
        // (DISABLED) super.testRepeatDailyFromCompleteDateCompleteAfter();
    }

    @Override
    public void testRepeatWeeklyFromCompleteDateCompleteBefore() {
        // (DISABLED) super.testRepeatWeeklyFromCompleteDateCompleteBefore();
    }

    @Override
    public void testRepeatWeeklyFromCompleteDateCompleteAfter() {
        // (DISABLED) super.testRepeatWeeklyFromCompleteDateCompleteAfter();
    }

    @Override
    public void testRepeatMonthlyFromCompleteDateCompleteBefore() {
        // (DISABLED) super.testRepeatMonthlyFromCompleteDateCompleteBefore();
    }

    @Override
    public void testRepeatMonthlyFromCompleteDateCompleteAfter() {
        // (DISABLED) super.testRepeatMonthlyFromCompleteDateCompleteAfter();
    }

    @Override
    public void testRepeatYearlyFromCompleteDateCompleteBefore() {
        // (DISABLED) super.testRepeatYearlyFromCompleteDateCompleteBefore();
    }

    @Override
    public void testRepeatYearlyFromCompleteDateCompleteAfter() {
        // (DISABLED) super.testRepeatYearlyFromCompleteDateCompleteAfter();
    }

    @Override
    public void testAdvancedRepeatWeeklyFromDueDateCompleteBefore() {
        // (DISABLED) super.testAdvancedRepeatWeeklyFromDueDateCompleteBefore();
    }

    @Override
    public void testAdvancedRepeatWeeklyFromDueDateCompleteAfter() {
        // (DISABLED) super.testAdvancedRepeatWeeklyFromDueDateCompleteAfter();
    }

    @Override
    public void testAdvancedRepeatWeeklyFromCompleteDateCompleteBefore() {
        // (DISABLED) super.testAdvancedRepeatWeeklyFromCompleteDateCompleteBefore();
    }

    @Override
    public void testAdvancedRepeatWeeklyFromCompleteDateCompleteAfter() {
        // (DISABLED) super.testAdvancedRepeatWeeklyFromCompleteDateCompleteAfter();
    }


}
