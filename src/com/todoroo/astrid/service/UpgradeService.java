package com.todoroo.astrid.service;

import java.util.HashMap;
import java.util.Map.Entry;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.task.AbstractTaskModel;
import com.todoroo.andlib.data.AbstractDao;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.model.Task;


public final class UpgradeService {

    @Autowired
    private Database database;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private String tasksTable;

    // --- implementation

    public UpgradeService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    /**
     * Perform upgrade from one version to the next. Needs to be called
     * on the UI thread so it can display a progress bar and then
     * show users a change log.
     *
     * @param from
     * @param to
     */
    public void performUpgrade(int from, int to) {
        if(from == to)
            return;

        if(from < 150) {
            upgrade2To3();
        }
    }

    // --- database upgrade logic

    /**
     * Upgrade helper class that reads a database
     */
    private class Astrid2UpgradeHelper extends SQLiteOpenHelper {

        private String name;

        public Astrid2UpgradeHelper(Context context, String name,
                CursorFactory factory, int version) {
            super(context, name, factory, version);
            this.name = name;
        }

        @Override
        @SuppressWarnings("nls")
        public void onCreate(SQLiteDatabase db) {
            // create empty table with nothing in it
            String sql = "CREATE TABLE IF NOT EXISTS " + name + " (" +
                AbstractModel.ID_PROPERTY + " INTEGER);";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // do nothing
        }

    }

    /**
     * Perform the upgrade from Astrid 2 to Astrid 3
     */
    private void upgrade2To3() {
        Context context = ContextManager.getContext();

        // --- upgrade tasks table
        HashMap<String, Property<?>> propertyMap =
            new HashMap<String, Property<?>>();
        propertyMap.put(AbstractController.KEY_ROWID, Task.ID);
        propertyMap.put(AbstractTaskModel.NAME, Task.TITLE);
        propertyMap.put(AbstractTaskModel.NOTES, Task.TITLE);
        // don't update progress percentage, we don't use this anymore
        propertyMap.put(AbstractTaskModel.IMPORTANCE, Task.IMPORTANCE);
        propertyMap.put(AbstractTaskModel.ESTIMATED_SECONDS, Task.ESTIMATED_SECONDS);
        propertyMap.put(AbstractTaskModel.ELAPSED_SECONDS, Task.ELAPSED_SECONDS);
        propertyMap.put(AbstractTaskModel.TIMER_START, Task.TIMER_START);
        propertyMap.put(AbstractTaskModel.DEFINITE_DUE_DATE, Task.DUE_DATE);
        propertyMap.put(AbstractTaskModel.PREFERRED_DUE_DATE, Task.PREFERRED_DUE_DATE);
        propertyMap.put(AbstractTaskModel.HIDDEN_UNTIL, Task.HIDDEN_UNTIL);
        propertyMap.put(AbstractTaskModel.POSTPONE_COUNT, Task.POSTPONE_COUNT);
        propertyMap.put(AbstractTaskModel.NOTIFICATIONS, Task.NOTIFICATIONS);
        propertyMap.put(AbstractTaskModel.NOTIFICATION_FLAGS, Task.NOTIFICATION_FLAGS);
        propertyMap.put(AbstractTaskModel.LAST_NOTIFIED, Task.LAST_NOTIFIED);
        propertyMap.put(AbstractTaskModel.REPEAT, Task.REPEAT);
        propertyMap.put(AbstractTaskModel.CREATION_DATE, Task.CREATION_DATE);
        propertyMap.put(AbstractTaskModel.COMPLETION_DATE, Task.COMPLETION_DATE);
        propertyMap.put(AbstractTaskModel.CALENDAR_URI, Task.CALENDAR_URI);
        propertyMap.put(AbstractTaskModel.FLAGS, Task.FLAGS);
        upgradeTasksTable(context, tasksTable,
                propertyMap, new Task(), taskDao);

        // --- upgrade tags table

    }

    protected static class UpgradeVisitorContainer {
        public int columnIndex;
        public Cursor cursor;
        public AbstractModel model;
    }

    /**
     * Visitor that reads from a visitor container and writes to the model
     * @author Tim Su <tim@todoroo.com>
     *
     */
    protected class ColumnUpgradeVisitor implements PropertyVisitor<Void, UpgradeVisitorContainer> {
        @Override
        public Void visitDouble(Property<Double> property, UpgradeVisitorContainer data) {
            double value = data.cursor.getDouble(data.columnIndex);
            data.model.setValue(property, value);
            return null;
        }

        @Override
        public Void visitInteger(Property<Integer> property, UpgradeVisitorContainer data) {
            int value;

            // convert long date -> integer
            if(property == Task.COMPLETION_DATE ||
                    property == Task.CREATION_DATE ||
                    property == Task.DELETION_DATE ||
                    property == Task.DUE_DATE ||
                    property == Task.HIDDEN_UNTIL ||
                    property == Task.LAST_NOTIFIED ||
                    property == Task.MODIFICATION_DATE ||
                    property == Task.PREFERRED_DUE_DATE)
                value = (int) (data.cursor.getLong(data.columnIndex) / 1000L);
            else
                value = data.cursor.getInt(data.columnIndex);

            data.model.setValue(property, value);
            return null;
        }

        @Override
        public Void visitLong(Property<Long> property, UpgradeVisitorContainer data) {
            long value = data.cursor.getLong(data.columnIndex);
            data.model.setValue(property, value);
            return null;
        }

        @Override
        public Void visitString(Property<String> property, UpgradeVisitorContainer data) {
            String value = data.cursor.getString(data.columnIndex);
            data.model.setValue(property, value);
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
    private <TYPE extends AbstractModel> void upgradeTasksTable(Context context, String legacyTable,
            HashMap<String, Property<?>> propertyMap, TYPE model,
            AbstractDao<TYPE> dao) {

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
            dao.createItem(database, container.model);
        }

        upgradeDb.close();
        context.deleteDatabase(legacyTable);
    }

}
