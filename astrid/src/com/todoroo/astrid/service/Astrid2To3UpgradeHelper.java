package com.todoroo.astrid.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

import com.timsu.astrid.R;
import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.alerts.Alert;
import com.timsu.astrid.data.task.AbstractTaskModel;
import com.timsu.astrid.utilities.TasksXmlExporter;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.alarms.Alarm;
import com.todoroo.astrid.alarms.AlarmDatabase;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.tags.TagService;

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
    private DialogUtilities dialogUtilities;

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

    /**
     * Perform the upgrade from Astrid 2 to Astrid 3
     */
    public void upgrade2To3() {
        Context context = ContextManager.getContext();

        // pop up a progress dialog
        ProgressDialog dialog = dialogUtilities.progressDialog(context, context.getString(R.string.DLG_wait));

        // initiate a backup
        try {
            TasksXmlExporter exporter = new TasksXmlExporter(true);
            exporter.setContext(ContextManager.getContext());
            exporter.exportTasks(TasksXmlExporter.getExportDirectory());
        } catch (Exception e) {
            // unable to create a backup before upgrading :(
        }

        database.openForWriting();

        // --- upgrade tasks table
        HashMap<String, Property<?>> propertyMap =
            new HashMap<String, Property<?>>();
        propertyMap.put(AbstractController.KEY_ROWID, Task.ID);
        propertyMap.put(AbstractTaskModel.NAME, Task.TITLE);
        propertyMap.put(AbstractTaskModel.NOTES, Task.NOTES);
        // don't update progress percentage, we don't use this anymore
        propertyMap.put(AbstractTaskModel.IMPORTANCE, Task.IMPORTANCE);
        propertyMap.put(AbstractTaskModel.ESTIMATED_SECONDS, Task.ESTIMATED_SECONDS);
        propertyMap.put(AbstractTaskModel.ELAPSED_SECONDS, Task.ELAPSED_SECONDS);
        propertyMap.put(AbstractTaskModel.TIMER_START, Task.TIMER_START);
        propertyMap.put(AbstractTaskModel.DEFINITE_DUE_DATE, Task.DUE_DATE);
        propertyMap.put(AbstractTaskModel.HIDDEN_UNTIL, Task.HIDE_UNTIL);
        propertyMap.put(AbstractTaskModel.POSTPONE_COUNT, Task.POSTPONE_COUNT);
        propertyMap.put(AbstractTaskModel.NOTIFICATIONS, Task.NOTIFICATIONS);
        propertyMap.put(AbstractTaskModel.NOTIFICATION_FLAGS, Task.NOTIFICATION_FLAGS);
        propertyMap.put(AbstractTaskModel.LAST_NOTIFIED, Task.LAST_NOTIFIED);
        propertyMap.put(AbstractTaskModel.REPEAT, Task.REPEAT);
        propertyMap.put(AbstractTaskModel.CREATION_DATE, Task.CREATION_DATE);
        propertyMap.put(AbstractTaskModel.COMPLETION_DATE, Task.COMPLETION_DATE);
        propertyMap.put(AbstractTaskModel.CALENDAR_URI, Task.CALENDAR_URI);
        propertyMap.put(AbstractTaskModel.FLAGS, Task.FLAGS);
        upgradeTable(context, tasksTable,
                propertyMap, new Task(), taskDao);

        // --- upgrade tags tables
        migrateTagsToMetadata();

        // --- upgrade alerts
        AlarmDatabase alarmsDatabase = new AlarmDatabase();
        alarmsDatabase.openForWriting();
        propertyMap.clear();
        propertyMap.put(AbstractController.KEY_ROWID, Alarm.ID);
        propertyMap.put(Alert.TASK, Alarm.TASK);
        propertyMap.put(Alert.DATE, Alarm.TIME);
        upgradeTable(context, alertsTable, propertyMap, new Alarm(),
                alarmsDatabase.getDao());
        alarmsDatabase.close();

        // --- upgrade RTM sync mappings (?)

        // --- clean up database
        metadataService.cleanup();

        database.close();

        dialog.dismiss();
    }

    // --- database upgrade helpers

    protected static final class UpgradeVisitorContainer {
        public int columnIndex;
        public Cursor cursor;
        public AbstractModel model;
        public StringBuilder upgradeNotes;
    }

    /**
     * Visitor that reads from a visitor container and writes to the model
     * @author Tim Su <tim@todoroo.com>
     *
     */
    @SuppressWarnings("nls")
    protected static final class ColumnUpgradeVisitor implements PropertyVisitor<Void, UpgradeVisitorContainer> {
        @Override
        public Void visitDouble(Property<Double> property, UpgradeVisitorContainer data) {
            double value = data.cursor.getDouble(data.columnIndex);
            data.model.setValue(property, value);
            Log.d("upgrade", "wrote " + value + " to -> " + property + " of model id " + data.cursor.getLong(1));
            return null;
        }

        @Override
        public Void visitInteger(Property<Integer> property, UpgradeVisitorContainer data) {
            int value = data.cursor.getInt(data.columnIndex);
            data.model.setValue(property, value);
            Log.d("upgrade", "wrote " + value + " to -> " + property + " of model id " + data.cursor.getLong(1));
            return null;
        }

        @Override
        public Void visitLong(Property<Long> property, UpgradeVisitorContainer data) {
            long value = data.cursor.getLong(data.columnIndex);

            // special handling for due date
            if(property == Task.DUE_DATE) {
                long preferredDueDate = data.cursor.getLong(data.cursor.getColumnIndex(AbstractTaskModel.PREFERRED_DUE_DATE));
                if(value == 0)
                    value = preferredDueDate;
                else if(preferredDueDate != 0) {
                    // had both absolute and preferred due dates. write
                    // preferred due date into notes field
                    if(data.upgradeNotes == null)
                        data.upgradeNotes = new StringBuilder();
                    data.upgradeNotes.append("Goal Deadline: " +
                            DateUtilities.getFormattedDate(ContextManager.getContext(),
                                    new Date(preferredDueDate)));
                }
            }

            data.model.setValue(property, value);
            Log.d("upgrade", "wrote " + value + " to -> " + property + " of model id " + data.cursor.getLong(1));
            return null;
        }

        @Override
        public Void visitString(Property<String> property, UpgradeVisitorContainer data) {
            String value = data.cursor.getString(data.columnIndex);
            data.model.setValue(property, value);
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
            GenericDao<TYPE> dao) {

        if(!checkIfDatabaseExists(context, legacyTable))
            return;

        SQLiteDatabase upgradeDb = new Astrid2UpgradeHelper(context, legacyTable,
                null, 1).getReadableDatabase();

        Cursor cursor = upgradeDb.rawQuery("SELECT * FROM " + legacyTable, null);
        UpgradeVisitorContainer container = new UpgradeVisitorContainer();
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
            }
            dao.createItem(container.model);
        }

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
        Cursor mapCursor = tagTaskDb.rawQuery("SELECT tag, task FROM " + tagTaskTable +
                " ORDER BY tag ASC", null);

        if(tagCursor.getCount() == 0)
            return;

        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, TagService.KEY);
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
                metadata.setValue(Metadata.KEY, TagService.KEY);
                metadata.setValue(Metadata.VALUE, tag);
                metadataDao.createItem(metadata);
                metadata.clearValue(Metadata.ID);
            }
        }

        tagCursor.close();
        mapCursor.close();
    }


}
