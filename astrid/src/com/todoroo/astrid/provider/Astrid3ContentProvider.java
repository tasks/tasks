/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.provider;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map.Entry;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.todoroo.andlib.data.AbstractDatabase;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Astrid Content Provider. Combines all Astrid tables into a single content
 * provider that can be queried, inserted into, and deleted from.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings({"nls","unused"})
public class Astrid3ContentProvider extends ContentProvider {

    static {
        AstridDependencyInjector.initialize();
    }

    /** URI for making a request over all tasks */
    private static final int URI_DIR = 1;

    /** URI for making a request over a single task by id */
    private static final int URI_ITEM = 2;

    /** URI for making a request over all tasks grouped by some field */
    private static final int URI_GROUP = 3;

    /** URI for making a request over all tasks grouped by some field */
    private static final int URI_DIR_WITH_METADATA = 4;

    protected static final String PROVIDER_NAME = AstridContentProvider.PROVIDER;

    // --- instance variables

    private final UriMatcher uriMatcher;

    @Autowired
    private AbstractDatabase database;

    @Autowired
    private ExceptionService exceptionService;

    /** Container classes for avoiding multiple object creation */
    private ContentValues taskValues, metadataValues, tempValues;

    /** List of task columns in a set form. Lazy initialized */
    private WeakReference<HashSet<String>> taskColumnSetRef = null;

    @Override
    public boolean onCreate() {
        try {
            ((Database)database).openForWriting(getContext());
            return database.getDatabase() != null;
        } catch (Exception e) {
            exceptionService.reportError("astrid-provider", e);
            return false;
        }
    }

    public ContentProvider() {
        DependencyInjectionService.getInstance().inject(this);

        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "items", URI_DIR);
        uriMatcher.addURI(PROVIDER_NAME, "#", URI_ITEM);
        uriMatcher.addURI(PROVIDER_NAME, "groupby/*", URI_GROUP);
        uriMatcher.addURI(PROVIDER_NAME, "itemsWith/*", URI_DIR_WITH_METADATA);

        setReadPermission(AstridApiConstants.PERMISSION_READ);
        setWritePermission(AstridApiConstants.PERMISSION_WRITE);
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
        case URI_DIR:
        case URI_GROUP:
        case URI_DIR_WITH_METADATA:
            return "vnd.android.cursor.dir/vnd.todoroo";
        case URI_ITEM:
            return "vnd.android.cursor/vnd.todoroo.item";
        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    protected Uri makeContentUri() {
        return Uri.parse("content://" + PROVIDER_NAME);
    }

    /* ======================================================================
     * =========================================================== delete ===
     * ====================================================================== */

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO
        return 0;
    }

    /* ======================================================================
     * =========================================================== insert ===
     * ====================================================================== */

    /**
     * Insert key/value pairs into metadata table with appropriate namespace.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (uriMatcher.match(uri)) {

        // illegal operations

        case URI_ITEM:
        case URI_GROUP:
        case URI_DIR_WITH_METADATA:
            throw new IllegalArgumentException("Only the allItems URI is valid"
                    + " for inserting a new task, I do not understand what you"
                    + " are trying to do!");

        // valid operations

        case URI_DIR: {
            // insert a task
            taskValues = initialize(taskValues);
            metadataValues = initialize(metadataValues);
            separateTaskColumnsFromMetadata(values, taskValues, metadataValues);

            // insert task columns, then insert metadata columns
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
        }

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /** Given a single ContentValues, separate into ones for tasks and ones
     * for metadata */
    private static void separateTaskColumnsFromMetadata(ContentValues values,
            ContentValues taskCols, ContentValues metadataCols) {
        // TODO
    }

    /** Initialize content provider */
    private synchronized ContentValues initialize(ContentValues values) {
        if(values == null)
            return new ContentValues();
        values.clear();
        return values;
    }

    /** Assert that the provided values contain the key given */
    private static void assertContains(ContentValues values, Property<?> key) {
        if(!values.containsKey(key.name)) {
            throw new IllegalArgumentException("ContentValues must contain key " + key);
        }
    }

    /* ======================================================================
     * =========================================================== update ===
     * ====================================================================== */

    /**
     * Undscapes a string for use in a URI. Used internally to pass extra data
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
     * Query by metadata.
     *
     * Note that the "sortOrder" field actually can be used to append any
     * sort of clause to your SQL query as long as it is not also the
     * name of a column
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        HashSet<String> taskColumnSet = initializeTaskColumnSet();

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        StringBuilder tables = new StringBuilder(Database.TASK_TABLE).append(" t");

        // walk through projection columns building tables and projections
        for(int i = 0; i < projection.length; i++) {
            if(taskColumnSet.contains(projection[i])) {
                String tableName = "m" + i;
                projection[i] = String.format("%s.%s AS %s",
                        tableName, Metadata.VALUE, projection[i]);
                tables.append(String.format(" LEFT OUTER JOIN %s %s ON t._id = %s.%s",
                        Database.METADATA_TABLE, tableName, tableName, Metadata.TASK.name));
                builder.appendWhereEscapeString(String.format("%s.%s = '%s' AND ",
                        tableName, Metadata.KEY.name, projection[i]));
            }
        }

        // add data from URI
        String groupBy = null;
        switch (uriMatcher.match(uri)) {
        case URI_GROUP:
            groupBy = uri.getPathSegments().get(1);
        case URI_DIR:
            builder.appendWhere("TRUE");
            break;
        case URI_ITEM:
            String itemSelector = String.format("%s = '%s'",
                    AstridTask.ID, uri.getPathSegments().get(1));
            builder.appendWhere(itemSelector);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor cursor = builder.query(database.getDatabase(), projection, selection, selectionArgs, groupBy, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /** helper method to lazy-initialize taskColumnSet */
    private synchronized HashSet<String> initializeTaskColumnSet() {
        if(taskColumnSetRef != null && taskColumnSetRef.get() != null)
            return taskColumnSetRef.get();

        HashSet<String> taskColumnSet = new HashSet<String>();
        taskColumnSetRef = new WeakReference<HashSet<String>>(taskColumnSet);
        for(Property<?> property : Task.PROPERTIES)
            taskColumnSet.add(property.qualifiedName());
        return taskColumnSet;
    }

}
