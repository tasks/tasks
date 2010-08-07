package com.todoroo.astrid.alarms;

import java.util.LinkedHashSet;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.service.MetadataService;

/**
 * Provides operations for working with alerts
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AlarmService {

    // --- singleton

    private static AlarmService instance = null;

    public static synchronized AlarmService getInstance() {
        if(instance == null)
            instance = new AlarmService();
        return instance;
    }

    // --- interface

    /**
     * Return alarms for the given task. PLEASE CLOSE THE CURSOR!
     *
     * @param taskId
     */
    public TodorooCursor<Metadata> getAlarms(long taskId) {
        return PluginServices.getMetadataService().query(Query.select(
                Metadata.PROPERTIES).where(MetadataCriteria.byTaskAndwithKey(
                        taskId, Alarm.METADATA_KEY)).orderBy(Order.asc(Alarm.TIME)));
    }

    /**
     * Save the given array of tags into the database
     * @param taskId
     * @param tags
     */
    public void synchronizeAlarms(long taskId, LinkedHashSet<Long> alarms) {
        MetadataService service = PluginServices.getMetadataService();
        service.deleteWhere(Criterion.and(MetadataCriteria.byTask(taskId),
                MetadataCriteria.withKey(Alarm.METADATA_KEY)));

        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, Alarm.METADATA_KEY);
        metadata.setValue(Metadata.TASK, taskId);
        for(Long alarm : alarms) {
            metadata.clearValue(Metadata.ID);
            metadata.setValue(Alarm.TIME, alarm);
            metadata.setValue(Alarm.TYPE, Alarm.TYPE_SINGLE);
            service.save(metadata);
        }
    }
}
