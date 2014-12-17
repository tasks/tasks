package org.tasks.timelog;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.dao.TaskTimeLogDao;
import com.todoroo.astrid.data.TaskTimeLog;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TimeLogService {


    private final TaskTimeLogDao timeLogDao;

    @Inject
    public TimeLogService(TaskTimeLogDao timeLogDao) {
        this.timeLogDao = timeLogDao;
    }

    public void getTimeLogs(long taskId, Callback<TaskTimeLog> callback) {
        timeLogDao.query(callback, Query.select(TaskTimeLog.PROPERTIES)
                .where(TaskTimeLogDao.TaskTimeLogCriteria.byTaskId(taskId))
                .orderBy(Order.desc(TaskTimeLog.TIME)));
    }

    public void addTimeLog(TaskTimeLog taskTimeLog){
        timeLogDao.createNew(taskTimeLog);
    }

    public boolean synchronizeTimeLogs(long taskId, Set<TaskTimeLog> timeLogs) {
        Set<TaskTimeLog> oldTimeLogs = Sets.newHashSet();
        TodorooCursor<TaskTimeLog> cursor = timeLogDao.query(Query
                .select(TaskTimeLog.PROPERTIES)
                .where(TaskTimeLogDao.TaskTimeLogCriteria.byTaskId(taskId)));
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                oldTimeLogs.add(new TaskTimeLog(cursor));
            }
        } finally {
            cursor.close();
        }
        Function<TaskTimeLog, String> getUidFunction = new Function<TaskTimeLog, String>() {
            @Override
            public String apply(@Nullable TaskTimeLog input) {
                return input.getUuid();
            }
        };
        ImmutableMap<String, TaskTimeLog> newTimeLogsMap = Maps.uniqueIndex(timeLogs, getUidFunction);
        ImmutableMap<String, TaskTimeLog> oldTimeLogsMap = Maps.uniqueIndex(oldTimeLogs, getUidFunction);

        Collection<String> toRemove = Sets.difference(oldTimeLogsMap.keySet(), newTimeLogsMap.keySet());
        Collection<String> toAdd = Sets.difference(newTimeLogsMap.keySet(), oldTimeLogsMap.keySet());
        Collection<String> toModify = Sets.intersection(oldTimeLogsMap.keySet(), newTimeLogsMap.keySet());


        for (String uid : toAdd) {
            TaskTimeLog taskTimeLog = newTimeLogsMap.get(uid);
            timeLogDao.createNew(taskTimeLog);
        }

        for (String uid : toRemove) {
            TaskTimeLog taskTimeLog = oldTimeLogsMap.get(uid);
            timeLogDao.delete(taskTimeLog.getId());
        }

        boolean taskChanged = !toAdd.isEmpty() || !toRemove.isEmpty();
        for (String uid : toModify) {
            TaskTimeLog taskTimeLog = newTimeLogsMap.get(uid);
            timeLogDao.saveExisting(taskTimeLog);
            TaskTimeLog oldTimeLog = oldTimeLogsMap.get(taskTimeLog.getUuid());
            if (!taskTimeLog.equals(oldTimeLog)) {
                taskChanged = true;
            }
        }

        return taskChanged;
    }

}
