/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms;

import android.content.ContentValues;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.SynchronizeMetadataCallback;

import org.tasks.injection.ApplicationScope;
import org.tasks.jobs.Alarm;
import org.tasks.jobs.JobQueue;
import org.tasks.jobs.JobQueueEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

/**
 * Provides operations for working with alerts
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@ApplicationScope
public class AlarmService {

    private static final long NO_ALARM = Long.MAX_VALUE;

    private final JobQueue jobs;

    private final MetadataDao metadataDao;

    @Inject
    public AlarmService(MetadataDao metadataDao, JobQueue jobQueue) {
        this.metadataDao = metadataDao;
        jobs = jobQueue;
    }

    public void getAlarms(long taskId, Callback<Metadata> callback) {
        metadataDao.query(callback, Query.select(
                Metadata.PROPERTIES).where(MetadataCriteria.byTaskAndwithKey(
                taskId, AlarmFields.METADATA_KEY)).orderBy(Order.asc(AlarmFields.TIME)));
    }

    /**
     * Save the given array of alarms into the database
     * @return true if data was changed
     */
    public boolean synchronizeAlarms(final long taskId, Set<Long> alarms) {
        List<Metadata> metadata = new ArrayList<>();
        for(Long alarm : alarms) {
            Metadata item = new Metadata();
            item.setKey(AlarmFields.METADATA_KEY);
            item.setValue(AlarmFields.TIME, alarm);
            item.setValue(AlarmFields.TYPE, AlarmFields.TYPE_SINGLE);
            metadata.add(item);
        }

        boolean changed = synchronizeMetadata(taskId, metadata, m -> jobs.cancelAlarm(m.getId()));

        if(changed) {
            scheduleAlarms(taskId);
        }
        return changed;
    }

    // --- alarm scheduling

    private void getActiveAlarms(Callback<Metadata> callback) {
        metadataDao.query(callback, Query.select(Metadata.PROPERTIES).
                join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
                where(Criterion.and(TaskCriteria.isActive(),
                        Task.REMINDER_LAST.lt(AlarmFields.TIME),
                        MetadataCriteria.withKey(AlarmFields.METADATA_KEY))));
    }

    private void getActiveAlarmsForTask(long taskId, Callback<Metadata> callback) {
        metadataDao.query(callback, Query.select(Metadata.PROPERTIES).
                join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
                where(Criterion.and(TaskCriteria.isActive(),
                        Task.REMINDER_LAST.lt(AlarmFields.TIME),
                        MetadataCriteria.byTaskAndwithKey(taskId, AlarmFields.METADATA_KEY))));
    }

    /**
     * Schedules all alarms
     */
    public void scheduleAllAlarms() {
        getActiveAlarms(this::scheduleAlarm);
    }

    /**
     * Schedules alarms for a single task
     */
    private void scheduleAlarms(long taskId) {
        getActiveAlarmsForTask(taskId, this::scheduleAlarm);
    }

    /**
     * Schedules alarms for a single task
     */
    private void scheduleAlarm(Metadata metadata) {
        if(metadata == null) {
            return;
        }

        Alarm alarm = new Alarm(metadata);
        long time = alarm.getTime();
        if(time == 0 || time == NO_ALARM) {
            jobs.cancelAlarm(alarm.getId());
        } else {
            jobs.add(alarm);
        }
    }

    private boolean synchronizeMetadata(long taskId, List<Metadata> metadata, final SynchronizeMetadataCallback callback) {
        final boolean[] dirty = new boolean[1];
        final Set<ContentValues> newMetadataValues = new HashSet<>();
        for(Metadata metadatum : metadata) {
            metadatum.setTask(taskId);
            metadatum.clearValue(Metadata.CREATION_DATE);
            metadatum.clearValue(Metadata.ID);

            ContentValues values = metadatum.getMergedValues();
            for(Map.Entry<String, Object> entry : values.valueSet()) {
                if(entry.getKey().startsWith("value")) //$NON-NLS-1$
                {
                    values.put(entry.getKey(), entry.getValue().toString());
                }
            }
            newMetadataValues.add(values);
        }

        metadataDao.byTaskAndKey(taskId, AlarmFields.METADATA_KEY, item -> {
            long id = item.getId();

            // clear item id when matching with incoming values
            item.clearValue(Metadata.ID);
            item.clearValue(Metadata.CREATION_DATE);
            ContentValues itemMergedValues = item.getMergedValues();

            if(newMetadataValues.contains(itemMergedValues)) {
                newMetadataValues.remove(itemMergedValues);
            } else {
                // not matched. cut it
                item.setId(id);
                if (callback != null) {
                    callback.beforeDeleteMetadata(item);
                }
                metadataDao.delete(id);
                dirty[0] = true;
            }
        });

        // everything that remains shall be written
        for(ContentValues values : newMetadataValues) {
            Metadata item = new Metadata();
            item.setCreationDate(DateUtilities.now());
            item.mergeWith(values);
            metadataDao.persist(item);
            dirty[0] = true;
        }

        return dirty[0];
    }
}
