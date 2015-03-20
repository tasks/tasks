/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.RemoteModelDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.UserActivity;

import org.tasks.BuildConfig;
import org.tasks.injection.InjectingContentProvider;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import dagger.Lazy;

/**
 * Astrid 3 Content Provider. There are two ways to use this content provider:
 * <ul>
 * <li>access it directly just like any other content provider
 * <li>use the DAO classes from the Astrid API library
 * </ul>
 * <p>
 * The following base URI's are supported:
 * <ul>
 * <li>content://com.todoroo.astrid/tasks - task data ({@link Task})
 * <li>content://com.todoroo.astrid/metadata - task metadata ({@link Metadata})
 * <li>content://com.todoroo.astrid/store - non-task store data ({@link StoreObject})
 * </ul>
 * <p>
 * Each URI supports the following components:
 * <ul>
 * <li>/ - operate on all items (insert, delete, update, query)
 * <li>/123 - operate on item id #123 (delete, update, query)
 * <li>/groupby/title - query with SQL "group by" (query)
 * </ul>
 * <p>
 * If you are writing a third-party application to access this data, you may
 * also consider using one of the Api DAO objects like TaskApiDao.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class Astrid3ContentProvider extends InjectingContentProvider {

    /** URI for making a request over all items */
    private static final int URI_DIR = 1;

    /** URI for making a request over a single item by id */
    private static final int URI_ITEM = 2;

    /** URI for making a request over all items grouped by some field */
    private static final int URI_GROUP = 3;

    private static final UriMatcher uriMatcher;

    private static Database databaseOverride;

    // --- instance variables

    private boolean open;
    @Inject Lazy<Database> database;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        for(Uri uri : new Uri[] { Task.CONTENT_URI, Metadata.CONTENT_URI, StoreObject.CONTENT_URI, UserActivity.CONTENT_URI }) {
            String authority = BuildConfig.APPLICATION_ID;

            String table = uri.toString();
            table = table.substring(table.indexOf('/', 11) + 1);

            uriMatcher.addURI(authority, table, URI_DIR);
            uriMatcher.addURI(authority, table + "/#", URI_ITEM);
            uriMatcher.addURI(authority, table +
                    AstridApiConstants.GROUP_BY_URI + "*", URI_GROUP);
        }
    }

    public Astrid3ContentProvider() {
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
            throw new IllegalArgumentException("Unsupported URI " + uri + " (" + uriMatcher.match(uri) + ")");
        }
    }

    /* ======================================================================
     * ========================================================== helpers ===
     * ====================================================================== */

    private class UriHelper<TYPE extends AbstractModel> {

        /** empty model. used for insert */
        public TYPE model;

        /** dao */
        public DatabaseDao<TYPE> dao;

        /** creates from given model */
        public boolean create() {
            return dao.createNew(model);
        }

        /** updates from given model */
        public void update() {
            dao.saveExisting(model);
        }

    }

    private UriHelper<?> generateHelper(Uri uri, boolean populateModel) {
        final Database db = getDatabase();
        if(uri.toString().startsWith(Task.CONTENT_URI.toString())) {
            UriHelper<Task> helper = new UriHelper<>();
            helper.model = populateModel ? new Task() : null;
            helper.dao = new RemoteModelDao<>(db, Task.class);
            return helper;
        } else if(uri.toString().startsWith(Metadata.CONTENT_URI.toString())) {
            UriHelper<Metadata> helper = new UriHelper<>();
            helper.model = populateModel ? new Metadata() : null;
            helper.dao = new DatabaseDao<>(db, Metadata.class);
            return helper;
        } else if(uri.toString().startsWith(StoreObject.CONTENT_URI.toString())) {
            UriHelper<StoreObject> helper = new UriHelper<>();
            helper.model = populateModel ? new StoreObject() : null;
            helper.dao = new DatabaseDao<>(db, StoreObject.class);
            return helper;
        } else if(uri.toString().startsWith(UserActivity.CONTENT_URI.toString())) {
            UriHelper<UserActivity> helper = new UriHelper<>();
            helper.model = populateModel ? new UserActivity() : null;
            helper.dao = new RemoteModelDao<>(db, UserActivity.class);
            return helper;
        }

        throw new UnsupportedOperationException("Unknown URI " + uri);
    }

    public static void setDatabaseOverride(Database override) {
        databaseOverride = override;
    }

    private Database getDatabase() {
        if (!open) {
            database.get().openForWriting();
            open = true;
        }
        if(databaseOverride != null) {
            return databaseOverride;
        }
        return database.get();
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
            if(TextUtils.isEmpty(selection)) {
                selection = itemSelector;
            } else {
                selection = itemSelector + " AND " + selection;
            }

        }

        case URI_DIR:
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri + " (" + uriMatcher.match(uri) + ")");
        }

        return getDatabase().delete(helper.dao.getTable().name, selection, selectionArgs);
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
            readTransitoriesFromModelContentValues(helper.model);
            if(!helper.create()) {
                throw new SQLException("Could not insert row into database (constraint failed?)");
            }

            Uri newUri = ContentUris.withAppendedId(uri, helper.model.getId());
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
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

        switch (uriMatcher.match(uri)) {

        // illegal operations

        case URI_GROUP:
            throw new IllegalArgumentException("Only the / or /# URI is valid"
                    + " for update.");

        // valid operations

        case URI_ITEM: {
            String itemSelector = String.format("%s = '%s'",
                    AbstractModel.ID_PROPERTY, uri.getPathSegments().get(1));
            if(TextUtils.isEmpty(selection)) {
                selection = itemSelector;
            } else {
                selection = itemSelector + " AND " + selection;
            }

        }

        case URI_DIR:
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri + " (" + uriMatcher.match(uri) + ")");
        }

        Cursor cursor = query(uri, new String[] { AbstractModel.ID_PROPERTY.name },
                selection, selectionArgs, null);
        try {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                helper.model.mergeWith(values);
                readTransitoriesFromModelContentValues(helper.model);
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

    private void readTransitoriesFromModelContentValues(AbstractModel model) {
        ContentValues setValues = model.getSetValues();
        if (setValues != null) {
            Set<Entry<String, Object>> entries = setValues.valueSet();
            Set<String> keysToRemove = new HashSet<>();
            for (Entry<String, Object> entry: entries) {
                String key = entry.getKey();
                if (key.startsWith(AbstractModel.RETAIN_TRANSITORY_PREFIX)) {
                    String newKey = key.substring(AbstractModel.RETAIN_TRANSITORY_PREFIX.length());
                    Object value = setValues.get(key);
                    model.putTransitory(newKey, value);
                    keysToRemove.add(key);
                }
            }

            for (String key : keysToRemove) {
                setValues.remove(key);
            }
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
            groupBy = uri.getPathSegments().get(2);
        case URI_DIR:
            break;
        case URI_ITEM:
            String itemSelector = String.format("%s = '%s'",
                    AbstractModel.ID_PROPERTY, uri.getPathSegments().get(1));
            builder.appendWhere(itemSelector);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri + " (" + uriMatcher.match(uri) + ")");
        }

        Cursor cursor = builder.query(getDatabase().getDatabase(), projection, selection, selectionArgs, groupBy, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    // --- change listeners

    public static void notifyDatabaseModification(Context context) {
        ContentResolver cr = context.getContentResolver();
        cr.notifyChange(Task.CONTENT_URI, null);
    }
}
