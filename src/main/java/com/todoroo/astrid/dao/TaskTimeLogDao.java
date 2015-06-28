package com.todoroo.astrid.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.TaskTimeLog;

import org.tasks.helper.UUIDHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaskTimeLogDao {

    private final RemoteModelDao<TaskTimeLog> dao;

    @Inject
    public TaskTimeLogDao(Database database) {
        dao = new RemoteModelDao<>(database, TaskTimeLog.class);
    }

    public static void migrateLoggedTime(SQLiteDatabase database){

        Property[] properties = {Task.ID, Task.ELAPSED_SECONDS, Task.COMPLETION_DATE, Task.CREATION_DATE, Task.UUID};
        List<String> strings = new ArrayList<>();
        for (Property property : properties) {
            strings.add(property.name);
        }
        Cursor cursor = database.query(Task.TABLE.name, strings.toArray(new String[0]), null, null, null, null, null);
        TodorooCursor<Task> todorooCursor = new TodorooCursor<>(cursor, properties);

        try {
            while (todorooCursor.moveToNext()){
                Task task = new Task(todorooCursor);
                TaskTimeLog taskTimeLog = createTimeLogFromTask(task);
                if (taskTimeLog != null) {
                    database.insert(TaskTimeLog.TABLE.name, AbstractModel.ID_PROPERTY.name, taskTimeLog.getMergedValues());
                }
}
        } finally {
            cursor.close();
        }
    }

    public static TaskTimeLog createTimeLogFromTask(Task task) {
        Integer elapsedSeconds = task.getElapsedSeconds();
        if (elapsedSeconds == null || elapsedSeconds == 0){
            return null;
        }
        TaskTimeLog taskTimeLog = new TaskTimeLog();
        taskTimeLog.setTaskId(task.getId());
        taskTimeLog.setTaskUuid(task.getUuid());
        taskTimeLog.setTime(isNonZero(task.getCompletionDate()) ? task.getCompletionDate() : task.getCreationDate());
        taskTimeLog.setTimeSpent(elapsedSeconds);
        taskTimeLog.setUuid(UUIDHelper.newUUID());
        taskTimeLog.setID(TaskTimeLog.NO_ID);

        Integer estimatedSeconds = task.getEstimatedSeconds();
        if (isNonZero(estimatedSeconds)){
            int remainingSeconds = Math.max(0, estimatedSeconds - elapsedSeconds);
            task.setRemainingSeconds(remainingSeconds);
        }

        return taskTimeLog;
    }

    public static boolean isNonZero(Number completionDate) {
        return (completionDate != null && completionDate.intValue() != 0);
    }

    public void persist(TaskTimeLog timeLog) {
        dao.persist(timeLog);
    }

    public void deleteWhere(Criterion criterion) {
        dao.deleteWhere(criterion);
    }

    public void query(Callback<TaskTimeLog> callback, Query query) {
        dao.query(callback, query);
    }

    public void createNew(TaskTimeLog taskTimeLog) {
        dao.createNew(taskTimeLog);
    }

    public TodorooCursor<TaskTimeLog> query(Query where) {
        return dao.query(where);
    }

    public void saveExisting(TaskTimeLog taskTimeLog) {
        dao.saveExisting(taskTimeLog);
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
    public boolean delete(long id) {
        return dao.delete(id);
    }
    public int deleteWhere(long TaskId) {
        return dao.deleteWhere(TaskTimeLogCriteria.byTaskId(TaskId));
    }

    public void byTask(long taskId, Callback<TaskTimeLog> callback) {
        dao.query(callback, Query.select(TaskTimeLog.PROPERTIES).where(TaskTimeLogCriteria.byTaskId(taskId)));
    }
}
