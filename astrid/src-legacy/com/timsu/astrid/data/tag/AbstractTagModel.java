/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.tag;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.timsu.astrid.data.LegacyAbstractController;
import com.timsu.astrid.data.LegacyAbstractModel;


/** Abstract model of a task. Subclasses implement the getters and setters
 * they are interested in.
 *
 * @author timsu
 *
 */
@SuppressWarnings("nls")
public abstract class AbstractTagModel extends LegacyAbstractModel {

    /** Version number of this model */
    static final int                   VERSION             = 1;

    // field names

    static final String                NAME                = "name";
    static final String                NOTES               = "notes";
    // reserved fields
    static final String                ICON                = "icon";
    static final String                PARENT              = "parent";
    static final String                FLAGS               = "flags";
    static final String                LOCATION_LAT        = "locationLat";
    static final String                LOCATION_LONG       = "locationLong";
    static final String                NOTIFICATIONS       = "notifications";
    // end reserved fields
    static final String                CREATION_DATE       = "creationDate";

    /** Default values container */
    private static final ContentValues defaultValues       = new ContentValues();

    static {
        defaultValues.put(NAME, "");
        defaultValues.put(NOTES, "");
        defaultValues.put(ICON, 0);
        defaultValues.put(PARENT, 0);
        defaultValues.put(FLAGS, 0);
        defaultValues.put(LOCATION_LAT, 0);
        defaultValues.put(LOCATION_LONG, 0);
        defaultValues.put(NOTIFICATIONS, 0);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- database helper

    /** Database Helper manages creating new tables and updating old ones */
    static class TagModelDatabaseHelper extends SQLiteOpenHelper {
        String tableName;
        Context context;

        TagModelDatabaseHelper(Context context, String databaseName, String tableName) {
            super(context, databaseName, null, VERSION);
            this.tableName = tableName;
            this.context = context;
        }

        @Override
        public synchronized void onCreate(SQLiteDatabase db) {
            String sql = new StringBuilder().
            append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (").
                append(LegacyAbstractController.KEY_ROWID).append(" integer primary key autoincrement, ").
                append(NAME).append(" text unique,").
                append(NOTES).append(" text,").
                append(ICON).append(" integer,").
                append(PARENT).append(" integer,").
                append(FLAGS).append(" integer,").
                append(LOCATION_LAT).append(" integer,").
                append(LOCATION_LONG).append(" integer,").
                append(NOTIFICATIONS).append(" integer,").
                append(CREATION_DATE).append(" integer").
            append(");").toString();
            db.execSQL(sql);
        }

        @Override
        public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(getClass().getSimpleName(), "Upgrading database from version " +
                    oldVersion + " to " + newVersion + ".");

            switch(oldVersion) {
            default:
                // we don't know how to handle it... show an error
                Log.e(getClass().getSimpleName(), "Unsupported migration from " + oldVersion + " to " + newVersion);
            }
        }
    }

    // --- utility methods



    // --- identifier

    private TagIdentifier identifier = null;

    public TagIdentifier getTagIdentifier() {
        return identifier;
    }

    void setTagIdentifier(TagIdentifier identifier) {
        this.identifier = identifier;
    }

    // --- constructor pass-through

    AbstractTagModel() {
        super();
    }

    /** Read identifier from database */
    AbstractTagModel(Cursor cursor) {
        super(cursor);

        Integer id = retrieveInteger(LegacyAbstractController.KEY_ROWID);
        setTagIdentifier(new TagIdentifier(id));
    }

    /** Get identifier from argument */
    AbstractTagModel(TagIdentifier identifier, Cursor cursor) {
        super(cursor);

        setTagIdentifier(identifier);
    }

    // --- getters and setters: expose them as you see fit

    protected String getName() {
        return retrieveString(NAME);
    }

    protected String getNotes() {
        return retrieveString(NOTES);
    }

    protected Date getCreationDate() {
        return retrieveDate(CREATION_DATE);
    }

    // --- setters

    protected void setName(String name) {
        setValues.put(NAME, name.trim());
    }

    protected void setNotes(String notes) {
        setValues.put(NOTES, notes);
    }

    protected void setCreationDate(Date creationDate) {
        putDate(setValues, CREATION_DATE, creationDate);
    }

    // --- utility methods

    static void putDate(ContentValues cv, String fieldName, Date date) {
        if(date == null)
            cv.put(fieldName, (Long)null);
        else
            cv.put(fieldName, date.getTime());
    }
}
