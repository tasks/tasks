package com.todoroo.astrid.dao;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.data.TaskTimeLog;

import org.tasks.Broadcaster;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaskTimeLogDao extends RemoteModelDao<TaskTimeLog> {

    @Inject
    public TaskTimeLogDao(Database database) {
        super(TaskTimeLog.class);
        setDatabase(database);
    }
    /**
     * Generates SQL clauses
     */
    public static class TaskTimeLogCriteria {

        /** @return Time Logs by id nadmiarowe! */
        public static Criterion byId(long id) {
            return TaskTimeLog.ID.eq(id);
        }

        /** @return Time Logs for selected task by TaskId */
        public static Criterion byTaskId(long TaskId) {
            return TaskTimeLog.TASK_ID.eq(TaskId);
        }

        /** @return Time Logs between Time1 and Time2 */
        public static Criterion betweenTimes(Date Time1, Date Time2) {
            return Criterion.and(TaskTimeLog.TIME.lte(Time2),
                    TaskTimeLog.TIME.gt(Time1));
        }

    }
    // --- delete

    /**
     * Delete the given item
     *
     * @return true if delete was successful
     */
    @Override
    public boolean delete(long id) {
        boolean result = super.delete(id);
        if(!result) {
            return false;
        }
        return true;
    }
    public int deleteWhere(long TaskId) {
        int result = deleteWhere(TaskTimeLogCriteria.byTaskId(TaskId));
        return result;
    }
}
