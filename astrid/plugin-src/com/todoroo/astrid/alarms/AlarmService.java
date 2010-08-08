package com.todoroo.astrid.alarms;

import java.util.Date;
import java.util.LinkedHashSet;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.reminders.Notifications;
import com.todoroo.astrid.reminders.ReminderService;
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

    // --- data retrieval

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

    // --- alarm scheduling

    /**
     * Gets a listing of all alarms that are active
     * @param properties
     * @return todoroo cursor. PLEASE CLOSE THIS CURSOR!
     */
    private TodorooCursor<Metadata> getActiveAlarms() {
        return PluginServices.getMetadataService().query(Query.select(Alarm.TIME).
                join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
                where(Criterion.and(TaskCriteria.isActive(), MetadataCriteria.withKey(Alarm.METADATA_KEY))));
    }

    /**
     * Gets a listing of alarms by task
     * @param properties
     * @return todoroo cursor. PLEASE CLOSE THIS CURSOR!
     */
    private TodorooCursor<Metadata> getAlarmsForTask(long taskId) {
        return PluginServices.getMetadataService().query(Query.select(Alarm.TIME).
                join(Join.inner(Task.TABLE, Metadata.TASK.eq(Task.ID))).
                where(Criterion.and(TaskCriteria.isActive(),
                        MetadataCriteria.byTaskAndwithKey(taskId, Alarm.METADATA_KEY))));
    }

    /**
     * Schedules all alarms
     */
    public void scheduleAllAlarms() {
        TodorooCursor<Metadata> cursor = getActiveAlarms();
        try {
            Metadata alarm = new Metadata();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                alarm.readFromCursor(cursor);
                scheduleAlarm(alarm);
            }
        } catch (Exception e) {
            // suppress
        } finally {
            cursor.close();
        }
    }

    private static final long NO_ALARM = Long.MAX_VALUE;

    /**
     * Schedules alarms for a single task
     * @param task
     */
    public void scheduleAlarms(Task task) {
        TodorooCursor<Metadata> cursor = getAlarmsForTask(task.getId());
        try {
            Metadata alarm = new Metadata();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                alarm.readFromCursor(cursor);
                scheduleAlarm(alarm);
            }
        } catch (Exception e) {
            // suppress
        } finally {
            cursor.close();
        }
    }

    /**
     * Schedules alarms for a single task
     *
     * @param shouldPerformPropertyCheck
     *            whether to check if task has requisite properties
     */
    @SuppressWarnings("nls")
    private void scheduleAlarm(Metadata alarm) {
        if(alarm == null)
            return;

        long taskId = alarm.getValue(Metadata.TASK);
        int type = ReminderService.TYPE_ALARM;

        Context context = ContextManager.getContext();
        Intent intent = new Intent(context, Notifications.class);
        intent.setType("ALARM" + Long.toString(taskId)); //$NON-NLS-1$
        intent.setAction(Integer.toString(type));
        intent.putExtra(Notifications.ID_KEY, taskId);
        intent.putExtra(Notifications.TYPE_KEY, type);

        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                intent, 0);

        long time = alarm.getValue(Alarm.TIME);
        if(time == 0 || time == NO_ALARM)
            am.cancel(pendingIntent);
        else {
            Log.e("Astrid", "Alarm (" + taskId + ", " + type +
                    ") set for " + new Date(time));
            am.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
    }
}
