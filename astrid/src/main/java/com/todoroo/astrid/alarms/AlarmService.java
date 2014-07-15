/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.data.TodorooCursor;
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
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.service.SynchronizeMetadataCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.injection.ForApplication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides operations for working with alerts
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Singleton
public class AlarmService {

    private static final Logger log = LoggerFactory.getLogger(AlarmService.class);

    // --- data retrieval

    private MetadataDao metadataDao;
    private final Context context;

    @Inject
    public AlarmService(MetadataDao metadataDao, @ForApplication Context context) {
        this.metadataDao = metadataDao;
        this.context = context;
    }

    /**
     * Return alarms for the given task. PLEASE CLOSE THE CURSOR!
     */
    public TodorooCursor<Metadata> getAlarms(long taskId) {
        return metadataDao.query(Query.select(
                Metadata.PROPERTIES).where(MetadataCriteria.byTaskAndwithKey(
                taskId, AlarmFields.METADATA_KEY)).orderBy(Order.asc(AlarmFields.TIME)));
    }

    /**
     * Save the given array of alarms into the database
     * @return true if data was changed
     */
    public boolean synchronizeAlarms(final long taskId, LinkedHashSet<Long> alarms) {
        ArrayList<Metadata> metadata = new ArrayList<>();
        for(Long alarm : alarms) {
            Metadata item = new Metadata();
            item.setKey(AlarmFields.METADATA_KEY);
            item.setValue(AlarmFields.TIME, alarm);
            item.setValue(AlarmFields.TYPE, AlarmFields.TYPE_SINGLE);
            metadata.add(item);
        }

        final AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        boolean changed = synchronizeMetadata(taskId, metadata, Metadata.KEY.eq(AlarmFields.METADATA_KEY), new SynchronizeMetadataCallback() {
            @Override
            public void beforeDeleteMetadata(Metadata m) {
                // Cancel the alarm before the metadata is deleted
                PendingIntent pendingIntent = pendingIntentForAlarm(m, taskId);
                am.cancel(pendingIntent);
            }
        });

        if(changed) {
            scheduleAlarms(taskId);
        }
        return changed;
    }

    // --- alarm scheduling

    /**
     * Gets a listing of all alarms that are active
     * @return todoroo cursor. PLEASE CLOSE THIS CURSOR!
     */
    private TodorooCursor<Metadata> getActiveAlarms() {
        return metadataDao.query(Query.select(Metadata.ID, Metadata.TASK, AlarmFields.TIME).
                join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
                where(Criterion.and(TaskCriteria.isActive(), MetadataCriteria.withKey(AlarmFields.METADATA_KEY))));
    }

    /**
     * Gets a listing of alarms by task
     * @return todoroo cursor. PLEASE CLOSE THIS CURSOR!
     */
    private TodorooCursor<Metadata> getActiveAlarmsForTask(long taskId) {
        return metadataDao.query(Query.select(Metadata.ID, Metadata.TASK, AlarmFields.TIME).
                join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
                where(Criterion.and(TaskCriteria.isActive(),
                        MetadataCriteria.byTaskAndwithKey(taskId, AlarmFields.METADATA_KEY))));
    }

    /**
     * Schedules all alarms
     */
    public void scheduleAllAlarms() {
        TodorooCursor<Metadata> cursor = getActiveAlarms();
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Metadata alarm = new Metadata(cursor);
                scheduleAlarm(alarm);
            }
        } catch (Exception e) {
            // suppress
            log.error(e.getMessage(), e);
        } finally {
            cursor.close();
        }
    }

    private static final long NO_ALARM = Long.MAX_VALUE;

    /**
     * Schedules alarms for a single task
     */
    public void scheduleAlarms(long taskId) {
        TodorooCursor<Metadata> cursor = getActiveAlarmsForTask(taskId);
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Metadata alarm = new Metadata(cursor);
                scheduleAlarm(alarm);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            cursor.close();
        }
    }

    private PendingIntent pendingIntentForAlarm(Metadata alarm, long taskId) {
        Intent intent = new Intent(context, Notifications.class);
        intent.setAction("ALARM" + alarm.getId()); //$NON-NLS-1$
        intent.putExtra(Notifications.ID_KEY, taskId);
        intent.putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_ALARM);

        return PendingIntent.getBroadcast(context, (int)alarm.getId(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Schedules alarms for a single task
     */
    private void scheduleAlarm(Metadata alarm) {
        if(alarm == null) {
            return;
        }

        long taskId = alarm.getTask();

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = pendingIntentForAlarm(alarm, taskId);

        long time = alarm.getValue(AlarmFields.TIME);
        if(time == 0 || time == NO_ALARM) {
            am.cancel(pendingIntent);
        } else if(time > DateUtilities.now()) {
            am.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
    }

    private boolean synchronizeMetadata(long taskId, ArrayList<Metadata> metadata,
                                       Criterion metadataCriterion, SynchronizeMetadataCallback callback) {
        boolean dirty = false;
        HashSet<ContentValues> newMetadataValues = new HashSet<>();
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

        TodorooCursor<Metadata> cursor = metadataDao.query(Query.select(Metadata.PROPERTIES).where(Criterion.and(MetadataCriteria.byTask(taskId),
                metadataCriterion)));
        try {
            // try to find matches within our metadata list
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Metadata item = new Metadata(cursor);
                long id = item.getId();

                // clear item id when matching with incoming values
                item.clearValue(Metadata.ID);
                item.clearValue(Metadata.CREATION_DATE);
                ContentValues itemMergedValues = item.getMergedValues();

                if(newMetadataValues.contains(itemMergedValues)) {
                    newMetadataValues.remove(itemMergedValues);
                    continue;
                }

                // not matched. cut it
                item.setId(id);
                if (callback != null) {
                    callback.beforeDeleteMetadata(item);
                }
                metadataDao.delete(id);
                dirty = true;
            }
        } finally {
            cursor.close();
        }

        // everything that remains shall be written
        for(ContentValues values : newMetadataValues) {
            Metadata item = new Metadata();
            item.setCreationDate(DateUtilities.now());
            item.mergeWith(values);
            metadataDao.persist(item);
            dirty = true;
        }

        return dirty;
    }
}
