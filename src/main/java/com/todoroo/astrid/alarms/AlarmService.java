/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

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
import com.todoroo.astrid.reminders.ReminderService;
import com.todoroo.astrid.service.SynchronizeMetadataCallback;

import org.tasks.injection.ForApplication;
import org.tasks.receivers.TaskNotificationReceiver;
import org.tasks.scheduling.AlarmManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final long NO_ALARM = Long.MAX_VALUE;

    private final MetadataDao metadataDao;
    private final Context context;
    private final AlarmManager alarmManager;
    private final Callback<Metadata> scheduleAlarm = new Callback<Metadata>() {
        @Override
        public void apply(Metadata alarm) {
            scheduleAlarm(alarm);
        }
    };

    @Inject
    public AlarmService(MetadataDao metadataDao, @ForApplication Context context, AlarmManager alarmManager) {
        this.metadataDao = metadataDao;
        this.context = context;
        this.alarmManager = alarmManager;
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

        boolean changed = synchronizeMetadata(taskId, metadata, new SynchronizeMetadataCallback() {
            @Override
            public void beforeDeleteMetadata(Metadata m) {
                // Cancel the alarm before the metadata is deleted
                PendingIntent pendingIntent = pendingIntentForAlarm(m, taskId);
                alarmManager.cancel(pendingIntent);
            }
        });

        if(changed) {
            scheduleAlarms(taskId);
        }
        return changed;
    }

    // --- alarm scheduling

    private void getActiveAlarms(Callback<Metadata> callback) {
        metadataDao.query(callback, Query.select(Metadata.ID, Metadata.TASK, AlarmFields.TIME).
                join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
                where(Criterion.and(TaskCriteria.isActive(), MetadataCriteria.withKey(AlarmFields.METADATA_KEY))));
    }

    private void getActiveAlarmsForTask(long taskId, Callback<Metadata> callback) {
        metadataDao.query(callback, Query.select(Metadata.ID, Metadata.TASK, AlarmFields.TIME).
                join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
                where(Criterion.and(TaskCriteria.isActive(),
                        MetadataCriteria.byTaskAndwithKey(taskId, AlarmFields.METADATA_KEY))));
    }

    /**
     * Schedules all alarms
     */
    public void scheduleAllAlarms() {
        getActiveAlarms(scheduleAlarm);
    }

    /**
     * Schedules alarms for a single task
     */
    private void scheduleAlarms(long taskId) {
        getActiveAlarmsForTask(taskId, scheduleAlarm);
    }

    private PendingIntent pendingIntentForAlarm(Metadata alarm, long taskId) {
        Intent intent = new Intent(context, TaskNotificationReceiver.class);
        intent.setAction("ALARM" + alarm.getId()); //$NON-NLS-1$
        intent.putExtra(TaskNotificationReceiver.ID_KEY, taskId);
        intent.putExtra(TaskNotificationReceiver.EXTRAS_TYPE, ReminderService.TYPE_ALARM);

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

        PendingIntent pendingIntent = pendingIntentForAlarm(alarm, taskId);

        long time = alarm.getValue(AlarmFields.TIME);
        if(time == 0 || time == NO_ALARM) {
            alarmManager.cancel(pendingIntent);
        } else if(time > DateUtilities.now()) {
            alarmManager.wakeupAdjustingForQuietHours(time, pendingIntent);
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

        metadataDao.byTaskAndKey(taskId, AlarmFields.METADATA_KEY, new Callback<Metadata>() {
            @Override
            public void apply(Metadata item) {
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
