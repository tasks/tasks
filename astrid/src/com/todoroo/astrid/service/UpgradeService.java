package com.todoroo.astrid.service;

import java.util.HashMap;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;
import android.webkit.WebView;

import com.timsu.astrid.R;
import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.task.AbstractTaskModel;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.PropertyVisitor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.tags.DataService;


public final class UpgradeService {

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private MetadataDao metadataDao;

    @Autowired
    private Database database;

    @Autowired
    private String tasksTable;

    @Autowired
    private String tagsTable;

    @Autowired
    private String tagTaskTable;

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
        if(from >= to || from < 1)
            return;

        // display changelog
        showChangeLog(from, to);
    }

    /**
     * Return a change log string. Releases occur often enough that we don't
     * expect change sets to be localized.
     *
     * @param from
     * @param to
     * @return
     */
    @SuppressWarnings("nls")
    public void showChangeLog(int from, int to) {
        StringBuilder changeLog = new StringBuilder("<html><body style='color: white'>");

        if(from <= 130)
            newVersionString(changeLog, "2.14.0 (5/24/10)", new String[] {
                    "Pick a calendar to 'Add to Calendar' (in Settings menu)",
                    "RTM: archived lists are ignored",
                    "Fixed user-reported crashes!"});
        if(from > 130 && from <= 131)
            newVersionString(changeLog, "2.14.1 (5/29/10)", new String[] {
                    "Fixed crash while using PureCalendar widget",
            });
        if(from > 130 && from <= 132)
            newVersionString(changeLog, "2.14.2 (5/29/10)", new String[] {
                    "Fixed crash when Polish version views completed tasks",
            });

        changeLog.append("</body></html>");

        WebView webView = new WebView(ContextManager.getContext());
        webView.loadData(changeLog.toString(), "text/html", "utf-8");
        webView.setBackgroundColor(0);

        new AlertDialog.Builder(ContextManager.getContext())
        .setTitle(R.string.UpS_changelog_title)
        .setView(webView)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setPositiveButton(android.R.string.ok, null)
        .show();
    }

    /**
     * Helper for adding a single version to the changelog
     * @param changeLog
     * @param version
     * @param changes
     */
    @SuppressWarnings("nls")
    private void newVersionString(StringBuilder changeLog, String version, String[] changes) {
        changeLog.append("<font style='text-align: center; color=#ffaa00'><b>Version ").append(version).append(":</b></font><br><ul>");
        for(String change : changes)
            changeLog.append("<li>").append(change).append("</li>\n");
        changeLog.append("</ul>");
    }

    // --- database upgrade logic

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
        upgradeTable(context, tasksTable,
                propertyMap, new Task(), taskDao);

        // --- upgrade tags tables
        migrateTagsToMetadata();

        database.close();

    }

    // --- database upgrade helpers

    protected static final class UpgradeVisitorContainer {
        public int columnIndex;
        public Cursor cursor;
        public AbstractModel model;
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
            Log.d("upgrade", "wrote " + value + " to -> " + property + " of model id " + data.cursor.getLong(1));
            return null;
        }

        @Override
        public Void visitLong(Property<Long> property, UpgradeVisitorContainer data) {
            long value = data.cursor.getLong(data.columnIndex);
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
        metadata.setValue(Metadata.KEY, DataService.KEY);
        long tagId = -1;
        String tag = null;
        for(mapCursor.moveToFirst(); !mapCursor.isAfterLast(); mapCursor.moveToNext()) {
            long mapTagId = mapCursor.getLong(1);

            while(mapTagId > tagId && !tagCursor.isLast()) {
                tagCursor.moveToNext();
                tagId = tagCursor.getLong(1);
            }

            if(mapTagId == tagId) {
                if(tag == null)
                    tag = tagCursor.getString(2);
                long task = mapCursor.getLong(2);
                metadata.setValue(Metadata.TASK, task);
                metadata.setValue(Metadata.VALUE, tag);
                metadataDao.createItem(metadata);
            }
        }

        tagCursor.close();
        mapCursor.close();
    }

}
