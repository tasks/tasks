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
import android.text.TextUtils;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.GenericDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.StoreObjectDao;
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
@SuppressWarnings("nls")
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
    private MetadataDao metadataDao;

    @Autowired
    private StoreObjectDao storeObjectDao;

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

        for(Uri uri : new Uri[] { Task.CONTENT_URI, Metadata.CONTENT_URI, StoreObject.CONTENT_URI }) {
            String uriAsString = uri.toString();
            uriMatcher.addURI(uriAsString, "", URI_DIR);
            uriMatcher.addURI(uriAsString, "#", URI_ITEM);
            uriMatcher.addURI(uriAsString,
                    AstridApiConstants.GROUP_BY_URI + "*", URI_GROUP);
        }

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
        if(uri.toString().startsWith(Task.CONTENT_URI.toString())) {
            UriHelper<Task> helper = new UriHelper<Task>();
            helper.model = populateModel ? new Task() : null;
            helper.dao = taskDao;
            return helper;
        } else if(uri.toString().startsWith(Metadata.CONTENT_URI.toString())) {
            UriHelper<Metadata> helper = new UriHelper<Metadata>();
            helper.model = populateModel ? new Metadata() : null;
            helper.dao = metadataDao;
            return helper;
        } else if(uri.toString().startsWith(StoreObject.CONTENT_URI.toString())) {
            UriHelper<StoreObject> helper = new UriHelper<StoreObject>();
            helper.model = populateModel ? new StoreObject() : null;
            helper.dao = storeObjectDao;
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
        UriHelper<?> helper = generateHelper(uri, false);
        switch (uriMatcher.match(uri)) {

        // illegal operations

        case URI_GROUP:
            throw new IllegalArgumentException("Only the / or /# URI is valid"
                    + " for deletion.");

        // valid operations

        case URI_ITEM: {
            String itemSelector = String.format("%s = '%s'",
                    AbstractModel.ID_PROPERTY, uri.getPathSegments().get(1));
            if(TextUtils.isEmpty(selection))
                selection = itemSelector;
            else
                selection = itemSelector + " AND " + selection;

        }

        case URI_DIR:
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        return database.delete(helper.dao.getTable().name, selection, selectionArgs);
    }

    /* ======================================================================
     * =========================================================== insert ===
     * ====================================================================== */

    /**
     * Insert key/value pairs into given table
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        UriHelper<?> helper = generateHelper(uri, true);
        switch (uriMatcher.match(uri)) {

        // illegal operations

        case URI_ITEM:
        case URI_GROUP:
            throw new IllegalArgumentException("Only the / URI is valid"
                    + " for insertion.");

        // valid operations

        case URI_DIR: {
            helper.model.mergeWith(values);
            helper.create();
            Uri newUri = ContentUris.withAppendedId(uri, helper.model.getId());
            getContext().getContentResolver().notifyChange(newUri, null);
        }

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /* ======================================================================
     * =========================================================== update ===
     * ====================================================================== */

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        UriHelper<?> helper = generateHelper(uri, true);

        Cursor cursor = query(uri, new String[] { AbstractModel.ID_PROPERTY.name },
                selection, selectionArgs, null);
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                helper.model.mergeWith(values);
                helper.model.setId(id);
                helper.update();
                helper.model.clear();
            }

            getContext().getContentResolver().notifyChange(uri, null);
            return cursor.getCount();
        } finally {
            cursor.close();
        }
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
