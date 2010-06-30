package com.todoroo.astrid.alarms;

import java.util.ArrayList;

import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Query;

/**
 * Provides operations for working with alerts
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class AlarmService {

    AlarmDatabase database = new AlarmDatabase();

    GenericDao<Alarm> dao = new GenericDao<Alarm>(Alarm.class, database);

    /**
     * Metadata key for # of alarms
     */
    public static final String ALARM_COUNT = "alarms-count";

    public AlarmService() {
        DependencyInjectionService.getInstance().inject(this);
    }


    /**
     * Return alarms for the given task
     *
     * @param taskId
     */
    public TodorooCursor<Alarm> getAlarms(long taskId) {
        database.openForReading();
        Query query = Query.select(Alarm.PROPERTIES).where(Alarm.TASK.eq(taskId));
        return dao.query(query);
    }

    /**
     * Save the given array of tags into the database
     * @param taskId
     * @param tags
     */
    public void synchronizeAlarms(long taskId, ArrayList<Alarm> alarms) {
        database.openForWriting();
        dao.deleteWhere(Alarm.TASK.eq(taskId));

        for(Alarm alarm : alarms) {
            alarm.setId(Alarm.NO_ID);
            alarm.setValue(Alarm.TASK, taskId);
            dao.saveItem(alarm);
        }
    }
}
