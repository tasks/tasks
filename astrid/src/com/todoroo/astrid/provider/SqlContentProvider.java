/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.service.AstridDependencyInjector;

/**
 * Non-public-API SQL content provider.
 *
 * This provider is dangerous and unsupported, use at your own risk. It allows
 * full SQL queries, which means LIMIT and JOIN in queries. Only SELECT is
 * currently supported.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class SqlContentProvider extends ContentProvider {

    // --- instance variables

    private static UriMatcher uriMatcher;

    static {
        AstridDependencyInjector.initialize();

        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(AstridApiConstants.API_PACKAGE + ".private",
                "sql", 0);
    }

    @Autowired
    private Database database;

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


    public SqlContentProvider() {
        DependencyInjectionService.getInstance().inject(this);

        setReadPermission(AstridApiConstants.PERMISSION_READ);
        setWritePermission(AstridApiConstants.PERMISSION_WRITE);
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
        case 0:
            return "vnd.android.cursor.dir/vnd.astrid";
        default:
            throw new IllegalArgumentException("Unsupported URI " + uri + " (" + uriMatcher.match(uri) + ")");
        }
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
        throw new UnsupportedOperationException("unimplemented");
    }

    /* ======================================================================
     * =========================================================== insert ===
     * ====================================================================== */

    /**
     * Insert key/value pairs into given table
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("unimplemented");
    }

    /* ======================================================================
     * =========================================================== update ===
     * ====================================================================== */

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException("unimplemented");
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

        Cursor cursor = database.rawQuery(selection, null);
        return cursor;
    }

}
