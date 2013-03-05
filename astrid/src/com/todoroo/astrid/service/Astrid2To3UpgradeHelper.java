/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.ical.values.RRule;
import com.timsu.astrid.R;
import com.timsu.astrid.utilities.LegacyTasksXmlExporter;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.alarms.AlarmFields;
import com.todoroo.astrid.backup.TasksXmlImporter;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.legacy.AlarmDatabase;
import com.todoroo.astrid.legacy.LegacyAlertModel;
import com.todoroo.astrid.legacy.LegacyRepeatInfo;
import com.todoroo.astrid.legacy.LegacyTaskModel;
import com.todoroo.astrid.legacy.TransitionalAlarm;
import com.todoroo.astrid.tags.TaskToTagMetadata;

public class Astrid2To3UpgradeHelper {

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private MetadataDao metadataDao;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private Database database;

    @Autowired
    private String tasksTable;

    @Autowired
    private String tagsTable;

    @Autowired
    private String tagTaskTable;

    @Autowired
    private String alertsTable;

    @Autowired
    private String syncTable;

    @Autowired
    private ExceptionService exceptionService;

    // --- implementation

    public Astrid2To3UpgradeHelper() {
        DependencyInjectionService.getInstance().inject(this);
    }


    /**
     * Upgrade helper class that reads a database
     */
    private static class Astrid2UpgradeHelper extends SQLiteOpenHelper {

        public Astrid2UpgradeHelper(Context context, String name,
                CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // do nothing
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // do nothing
        }

    }

    // ---------------------------------------------------------------- 3 => 3.1

    /**
     * Perform the upgrade from Astrid 3 to 3.1
     * @param context
     * @param upgradeService
     * @param from
     */
    public void upgrade3To3_1(final Context context, final int from) {
        if(!checkIfDatabaseExists(context, alertsTable))
            return;

        database.openForWriting();
        migrateAlarmsToMetadata();
    }

    // ----------------------------------------------------------------- 2 => 3

    /**
     * Perform the upgrade from Astrid 2 to Astrid 3
     * @param context2
     */
    @SuppressWarnings("deprecation")
    public void upgrade2To3(final Context context, final int from) {

        // if from < 1 (we don't know what version, and database exists, leave it alone)
        if(from < 1 && checkIfDatabaseExists(context, database.getName()))
            return;

        // if you don't have a legacy task table, skip this step
        if(!checkIfDatabaseExists(context, tasksTable))
            return;

        // else, if there's already a database table, clear it out (!!!)
        if(checkIfDatabaseExists(context, database.getName()))
            context.deleteDatabase(database.getName());
        database.openForWriting();

        // initiate a backup
        String backupFile = legacyBackup();

        try {

            // --- upgrade tasks table
            HashMap<String, Property<?>> propertyMap =
                new HashMap<String, Property<?>>();
            propertyMap.put("_id", Task.ID); //$NON-NLS-1$
            propertyMap.put(LegacyTaskModel.NAME, Task.TITLE);
            propertyMap.put(LegacyTaskModel.NOTES, Task.NOTES);
            // (don't update progress percentage, we don't use this anymore)
            propertyMap.put(LegacyTaskModel.IMPORTANCE, Task.IMPORTANCE);
            propertyMap.put(LegacyTaskModel.ESTIMATED_SECONDS, Task.ESTIMATED_SECONDS);
            propertyMap.put(LegacyTaskModel.ELAPSED_SECONDS, Task.ELAPSED_SECONDS);
            propertyMap.put(LegacyTaskModel.TIMER_START, Task.TIMER_START);
            propertyMap.put(LegacyTaskModel.DEFINITE_DUE_DATE, Task.DUE_DATE);
            propertyMap.put(LegacyTaskModel.HIDDEN_UNTIL, Task.HIDE_UNTIL);
            propertyMap.put(LegacyTaskModel.POSTPONE_COUNT, Task.POSTPONE_COUNT);
            propertyMap.put(LegacyTaskModel.NOTIFICATIONS, Task.REMINDER_PERIOD);
            propertyMap.put(LegacyTaskModel.NOTIFICATION_FLAGS, Task.REMINDER_FLAGS);
            propertyMap.put(LegacyTaskModel.LAST_NOTIFIED, Task.REMINDER_LAST);
            propertyMap.put(LegacyTaskModel.REPEAT, Task.RECURRENCE);
            propertyMap.put(LegacyTaskModel.CREATION_DATE, Task.CREATION_DATE);
            propertyMap.put(LegacyTaskModel.COMPLETION_DATE, Task.COMPLETION_DATE);
            propertyMap.put(LegacyTaskModel.CALENDAR_URI, Task.CALENDAR_URI);
            propertyMap.put(LegacyTaskModel.FLAGS, Task.FLAGS);
            upgradeTable(context, tasksTable,
                    propertyMap, new Task(), taskDao);

            // --- upgrade tags tables
            migrateTagsToMetadata();

            // --- upgrade alerts
            AlarmDatabase alarmsDatabase = new AlarmDatabase();
            alarmsDatabase.openForWriting();
            propertyMap.clear();
            propertyMap.put("_id", TransitionalAlarm.ID); //$NON-NLS-1$
            propertyMap.put(LegacyAlertModel.TASK, TransitionalAlarm.TASK);
            propertyMap.put(LegacyAlertModel.DATE, TransitionalAlarm.TIME);
            upgradeTable(context, alertsTable, propertyMap, new TransitionalAlarm(),
                    alarmsDatabase.getDao());
            alarmsDatabase.close();

            // --- clean up database
            metadataService.cleanup();

            // --- upgrade properties
            SharedPreferences prefs = Preferences.getPrefs(context);
            Editor editor = prefs.edit();
            int random = Preferences.getIntegerFromString(R.string.p_rmd_default_random_hours, -1);
            if(random != -1) {
                // convert days => hours
                editor.putString(context.getString(R.string.p_rmd_default_random_hours),
                        Integer.toString(random * 24));
            }
        } catch (Exception e) {
            exceptionService.reportError("backup-error", e); //$NON-NLS-1$
            if(backupFile != null) {
                // try to restore the latest XML
                TasksXmlImporter.importTasks(context, backupFile, null);
            }
        }
    }

    // --- database upgrade helpers

    /**
     * Create a legacy backup file
     */
    private String legacyBackup() {
        try {
            LegacyTasksXmlExporter exporter = new LegacyTasksXmlExporter(true);
            exporter.setContext(ContextManager.getContext());
            return exporter.exportTasks(LegacyTasksXmlExporter.getExportDirectory());
        } catch (Exception e) {
            // unable to create a backup before upgrading :(
            return null;
        }
    }

    protected static final class UpgradeVisitorContainer<TYPE extends AbstractModel> {
        public int columnIndex;
        public Cursor cursor;
        public TYPE model;
        public StringBuilder upgradeNotes;
    }

    /**
     * Visitor that reads from a visitor container and writes to the model
     * @author Tim Su <tim@todoroo.com>
     *
     */
    @SuppressWarnings("nls")
    protected static final class ColumnUpgradeVisitor implements PropertyVisitor<Void, UpgradeVisitorContainer<?>> {
        @Override
        public Void visitDouble(Property<Double> property, UpgradeVisitorContainer<?> data) {
            double value = data.cursor.getDouble(data.columnIndex);
            data.model.setValue(property, value);
            Log.d("upgrade", "wrote " + value + " to -> " + property + " of model id " + data.cursor.getLong(1));
            return null;
        }

        @Override
        public Void visitInteger(Property<Integer> property, UpgradeVisitorContainer<?> data) {
            int value = data.cursor.getInt(data.columnIndex);
            data.model.setValue(property, value);
            Log.d("upgrade", "wrote " + value + " to -> " + property + " of model id " + data.cursor.getLong(1));
            return null;
        }

        @Override
        public Void visitLong(Property<Long> property, UpgradeVisitorContainer<?> data) {
            long value = data.cursor.getLong(data.columnIndex);

            // special handling for due date
            if(property == Task.DUE_DATE) {
                long preferredDueDate = data.cursor.getLong(data.cursor.getColumnIndex(LegacyTaskModel.PREFERRED_DUE_DATE));
                if(value == 0)
                    value = preferredDueDate;
                else if(preferredDueDate != 0) {
                    // had both absolute and preferred due dates. write
                    // preferred due date into notes field
                    if(data.upgradeNotes == null)
                        data.upgradeNotes = new StringBuilder();
                    data.upgradeNotes.append("Goal Deadline: " +
                            DateUtilities.getDateString(ContextManager.getContext(),
                                    new Date(preferredDueDate)));
                }
            } else if(property == Task.REMINDER_PERIOD) {
                // old period was stored in seconds
                value *= 1000L;
            } else if(property == Task.COMPLETION_DATE) {
                // check if the task was actually completed
                int progress = data.cursor.getInt(data.cursor.getColumnIndex(LegacyTaskModel.PROGRESS_PERCENTAGE));
                if(progress < 100)
                    value = 0;
            }

            data.model.setValue(property, value);
            Log.d("upgrade", "wrote " + value + " to -> " + property + " of model id " + data.cursor.getLong(1));
            return null;
        }

        @Override
        public Void visitString(Property<String> property, UpgradeVisitorContainer<?> data) {
            String value = data.cursor.getString(data.columnIndex);

            if(property == Task.RECURRENCE) {
                LegacyRepeatInfo repeatInfo = LegacyRepeatInfo.fromSingleField(data.cursor.getInt(data.columnIndex));
                if(repeatInfo == null)
                    data.model.setValue(property, "");
                else {
                    RRule rrule = repeatInfo.toRRule();
                    data.model.setValue(property, rrule.toIcal());
                }
            } else {
                data.model.setValue(property, value);
            }

            Log.d("upgrade", "wrote " + value + " to -> " + property + " of model id " + data.cursor.getLong(1));
            return null;
        }
    }

    /**
     * Helper that reads entries from legacy database and row-by-row
     * creates new models and saves them.
     *
     * @param context
     * @param legacyTable
     * @param propertyMap
     * @param model
     * @param dao
     */
    @SuppressWarnings("nls")
    private static final <TYPE extends AbstractModel> void upgradeTable(Context context, String legacyTable,
            HashMap<String, Property<?>> propertyMap, TYPE model,
            DatabaseDao<TYPE> dao) {

        if(!checkIfDatabaseExists(context, legacyTable))
            return;

        SQLiteDatabase upgradeDb = new Astrid2UpgradeHelper(context, legacyTable,
                null, 1).getReadableDatabase();

        Cursor cursor = upgradeDb.rawQuery("SELECT * FROM " + legacyTable, null);
        UpgradeVisitorContainer<TYPE> container = new UpgradeVisitorContainer<TYPE>();
        container.cursor = cursor;
        container.model = model;
        ColumnUpgradeVisitor visitor = new ColumnUpgradeVisitor();
        for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            model.clear();
            for(Entry<String, Property<?>> entry : propertyMap.entrySet()) {
                container.columnIndex = cursor.getColumnIndex(entry.getKey());
                entry.getValue().accept(visitor, container);
            }

            // special tweak for adding upgrade notes to tasks
            if(container.upgradeNotes != null) {
                if(container.model.getValue(Task.NOTES).length() == 0)
                    container.model.setValue(Task.NOTES, container.upgradeNotes.toString());
                else {
                    container.model.setValue(Task.NOTES,
                            container.model.getValue(Task.NOTES) + "\n\n" +
                            container.upgradeNotes);
                }
                container.upgradeNotes = null;
            }
            dao.createNew(container.model);
        }
        cursor.close();

        upgradeDb.close();
    }

    private static boolean checkIfDatabaseExists(Context context, String legacyTable) {
        return context.getDatabasePath(legacyTable).exists();
    }

    /**
     * Move data from tags tables into metadata table. We do this by looping
     * through both the tags and tagTaskMap databases, reading data from
     * both and adding to the Metadata table. This way, we are able to
     * do everything in one pass without loading too much into memory
     */
    @SuppressWarnings("nls")
    private void migrateTagsToMetadata() {
        Context context = ContextManager.getContext();

        if(!checkIfDatabaseExists(context, tagsTable) ||
                !checkIfDatabaseExists(context, tagTaskTable))
            return;

        SQLiteDatabase tagsDb = new Astrid2UpgradeHelper(context, tagsTable,
                null, 1).getReadableDatabase();
        SQLiteDatabase tagTaskDb = new Astrid2UpgradeHelper(context, tagTaskTable,
                null, 1).getReadableDatabase();

        Cursor tagCursor = tagsDb.rawQuery("SELECT _id, name FROM " + tagsTable +
                " ORDER BY _id ASC", null);
        Cursor mapCursor = null;
        try {
            mapCursor = tagTaskDb.rawQuery("SELECT tag, task FROM " + tagTaskTable +
                " ORDER BY tag ASC", null);
            if(tagCursor.getCount() == 0)
                return;

            Metadata metadata = new Metadata();
            metadata.setValue(Metadata.KEY, TaskToTagMetadata.KEY);
            long tagId = -1;
            String tag = null;
            for(mapCursor.moveToFirst(); !mapCursor.isAfterLast(); mapCursor.moveToNext()) {
                long mapTagId = mapCursor.getLong(0);

                while(mapTagId > tagId && !tagCursor.isLast()) {
                    tagCursor.moveToNext();
                    tagId = tagCursor.getLong(0);
                    tag = null;
                }

                if(mapTagId == tagId) {
                    if(tag == null)
                        tag = tagCursor.getString(1);
                    long task = mapCursor.getLong(1);
                    metadata.setValue(Metadata.TASK, task);
                    metadata.setValue(Metadata.KEY, TaskToTagMetadata.KEY);
                    metadata.setValue(TaskToTagMetadata.TAG_NAME, tag);
                    metadataDao.createNew(metadata);
                    metadata.clearValue(Metadata.ID);
                }
            }
        } finally {
            tagCursor.close();
            if(mapCursor != null)
                mapCursor.close();
            tagsDb.close();
            tagTaskDb.close();
        }
    }

    /**
     * Move data from alert table into metadata table.
     */
    private void migrateAlarmsToMetadata() {
        Context context = ContextManager.getContext();

        if(!checkIfDatabaseExists(context, AlarmDatabase.NAME))
            return;

        AlarmDatabase alarmsDatabase = new AlarmDatabase();
        DatabaseDao<TransitionalAlarm> dao = new DatabaseDao<TransitionalAlarm>(
                TransitionalAlarm.class, alarmsDatabase);

        TodorooCursor<TransitionalAlarm> cursor = dao.query(Query.select(TransitionalAlarm.PROPERTIES));
        try {
            if(cursor.getCount() == 0)
                return;

            Metadata metadata = new Metadata();
            metadata.setValue(Metadata.KEY, AlarmFields.METADATA_KEY);
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long task = cursor.get(TransitionalAlarm.TASK);
                long time = cursor.get(TransitionalAlarm.TIME);

                metadata.setValue(Metadata.TASK, task);
                metadata.setValue(AlarmFields.TIME, time);
                metadata.setValue(AlarmFields.TYPE, AlarmFields.TYPE_SINGLE);
                metadataDao.createNew(metadata);
                metadata.clearValue(Metadata.ID);
            }
        } finally {
            cursor.close();
            alarmsDatabase.close();
        }
    }


}
