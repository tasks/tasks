/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.StoreObject;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Astrid 3 Content Provider. There are two ways to use this content provider:
 * <ul>
 * <li>access it directly just like any other content provider
 * <li>use the DAO classes from the Astrid API library
 * </ul>
 *
 * The following base URI's are supported:
 * <ul>
 * <li>content://com.todoroo.astrid/tasks - task data ({@link Task})
 * <li>content://com.todoroo.astrid/metadata - task metadata ({@link Metadata})
 * <li>content://com.todoroo.astrid/store - non-task store data ({@link StoreObject})
 * </ul>
 *
 * Each URI supports the following components:
 * <ul>
 * <li>/ - query for all items
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings({"nls","unused"})
public class Astrid3ContentProvider extends ContentProvider {

    static {
        AstridDependencyInjector.initialize();
    }

    /** URI for making a request over all items */
    private static final int URI_DIR = 1;

    /** URI for making a request over a single item by id */
    private static final int URI_ITEM = 2;

    /** URI for making a request over all items grouped by some field */
    private static final int URI_GROUP = 3;

    // --- instance variables

    private final UriMatcher uriMatcher;

    @Autowired
    private Database database;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private ExceptionService exceptionService;

    @Override
    public boolean onCreate() {
        try {
            database.openForWriting();
            return database.getDatabase() != null;
        } catch (Exception e) {
            exceptionService.reportError("astrid-provider", e);
            return false;
        }
    }

    public Astrid3ContentProvider() {
        DependencyInjectionService.getInstance().inject(this);

        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(Task.CONTENT_URI, "", URI_DIR);
        uriMatcher.addURI(Task.CONTENT_URI, "#", URI_ITEM);
        uriMatcher.addURI(Task.CONTENT_URI, "groupby/*", URI_GROUP);

        setReadPermission(AstridApiConstants.PERMISSION_READ);
        setWritePermission(AstridApiConstants.PERMISSION_WRITE);
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
        case URI_DIR:
        case URI_GROUP:
            return "vnd.android.cursor.dir/vnd.astrid";
        case URI_ITEM:
            return "vnd.android.cursor/vnd.astrid.item";
        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    /* ======================================================================
     * ========================================================== helpers ===
     * ====================================================================== */

    private class UriHelper<TYPE extends AbstractModel> {

        /** empty model. used for insert */
        public TYPE model;

        /** dao */
        public GenericDao<TYPE> dao;

        /** creates from given model */
        public void create() {
            dao.createNew(model);
        }

        /** updates from given model */
        public void update() {
            dao.saveExisting(model);
        }

    }

    private UriHelper<?> generateHelper(Uri uri, boolean populateModel) {
        if(uri.toString().startsWith(Task.CONTENT_URI)) {
            UriHelper<Task> helper = new UriHelper<Task>();
            helper.model = populateModel ? new Task() : null;
            helper.dao = taskDao;
            return helper;
        }

        throw new UnsupportedOperationException("Unknown URI " + uri);
    }

    /* ======================================================================
     * =========================================================== delete ===
     * ====================================================================== */

    /**
     * Delete from given table
     * @return number of rows deleted
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {

        // illegal operations

        case URI_GROUP:
            throw new IllegalArgumentException("Only the / or /# URI is valid"
                    + " for deletion.");

        // valid operations

        case URI_ITEM: {

            return 0;
        }

        case URI_DIR: {

            return 0;
        }

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /* ======================================================================
     * =========================================================== insert ===
     * ====================================================================== */

    /**
     * Insert key/value pairs into given table
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (uriMatcher.match(uri)) {

        // illegal operations

        case URI_ITEM:
        case URI_GROUP:
            throw new IllegalArgumentException("Only the / URI is valid"
                    + " for insertion.");

        // valid operations

        case URI_DIR: {

            UriHelper<?> helper = generateHelper(uri, true);
            helper.model.mergeWith(values);
            helper.create();

            return ContentUris.withAppendedId(uri, helper.model.getId());
        }

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /* ======================================================================
     * =========================================================== update ===
     * ====================================================================== */

    /**
     * Unescapes a string for use in a URI. Used internally to pass extra data
     * to the content provider.
     * @param component
     * @return
     */
    private static String unescapeUriComponent(String component) {
        return component.replace("%s", "/").replace("%o", "%");
    }

    /**
     * Escapes a string for use as part of a URI string. Used internally to pass extra data
     * to the content provider.
     * @param component
     * @return
     */
    private static String[] unpackUriSubComponent(String component) {
        return component.replace("$s", "|").replace("o", "$").split("|");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        /*switch (uriMatcher.match(uri)) {
        case URI_DIR_WITH_METADATA: {
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            String[] metadata = unpackUriSubComponent(uri.getPathSegments().get(1));
            StringBuilder tables = new StringBuilder(Database.TASK_TABLE).append(" t");
            for(int i = 0; i < metadata.length; i++) {
                String tableName = "m" + i;
                tables.append(String.format(" LEFT OUTER JOIN %s %s ON t._id = %s.%s",
                        Database.METADATA_TABLE, tableName, tableName, Metadata.TASK.name));
                builder.appendWhereEscapeString(String.format("%s.%s = '%s' AND ",
                        tableName, Metadata.KEY.name, projection[i]));
            }
        }

        case URI_DIR: {
            // UPDATE tasks SET ... WHERE ...
            taskValues = initialize(taskValues);
            metadataValues = initialize(metadataValues);
            separateTaskColumnsFromMetadata(values, taskValues, metadataValues);

            count = database.getDatabase().update(getTableName(), valuesWithDefaults,
                    selection, selectionArgs);

            // SELECT _id WHERE ...

            // INSERT OR REPLACE INTO metadata (...) VALUES (...)
            long taskId = TodorooContentProvider.insertHelper(database,
                    Database.TASK_TABLE, taskValues, Task.getStaticDefaultValues());

            for(Entry<String,Object> metadata : metadataValues.valueSet()) {
                tempValues = initialize(tempValues);
                tempValues.put(Metadata.KEY.name, metadata.getKey());
                tempValues.put(Metadata.VALUE.name, metadata.getValue().toString());
                TodorooContentProvider.insertHelper(database,
                        Database.METADATA_TABLE, tempValues, null);
            }

            if (taskId > 0) {
                Uri _uri = ContentUris.withAppendedId(makeContentUri(), taskId);
                getContext().getContentResolver().notifyChange(_uri, null);
                return _uri;
            }

            break;
        }
        case ITEM:
            String id = uri.getPathSegments().get(0);
            ContentValues newValues = new ContentValues();
            rewriteKeysFor(values, newValues);
            count = database.getDatabase().update(
                    getTableName(),
                    newValues,
                    (AbstractModel.ID_PROPERTY + "=" + id)
                            + (!TextUtils.isEmpty(selection) ? " AND ("
                                    + selection + ')' : ""), selectionArgs);
            break;
        case GROUP:
            throw new IllegalArgumentException("Invalid URI for update: " + uri);

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;*/

        return 0; // FIXME
    }

    /* ======================================================================
     * ============================================================ query ===
     * ====================================================================== */

    /**
     * Query by task.
     * <p>
     * Note that the "sortOrder" field actually can be used to append any
     * sort of clause to your SQL query as long as it is not also the
     * name of a column
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        String groupBy = null;

        UriHelper<?> helper = generateHelper(uri, false);
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(helper.dao.getTable().name);

        switch (uriMatcher.match(uri)) {
        case URI_GROUP:
            groupBy = uri.getPathSegments().get(1);
        case URI_DIR:
            break;
        case URI_ITEM:
            String itemSelector = String.format("%s = '%s'",
                    AbstractModel.ID_PROPERTY, uri.getPathSegments().get(1));
            builder.appendWhere(itemSelector);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor cursor = builder.query(database.getDatabase(), projection, selection, selectionArgs, groupBy, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

}
