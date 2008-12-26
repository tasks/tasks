/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.data.tag;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.timsu.astrid.data.AbstractController;
import com.timsu.astrid.data.AbstractModel;


/** Abstract model of a task. Subclasses implement the getters and setters
 * they are interested in.
 *
 * @author timsu
 *
 */
public abstract class AbstractTagModel extends AbstractModel {

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
        defaultValues.put(ICON, (Integer)null);
        defaultValues.put(PARENT, (Long)null);
        defaultValues.put(FLAGS, (Integer)0);
        defaultValues.put(LOCATION_LAT, (Integer)null);
        defaultValues.put(LOCATION_LONG, (Integer)null);
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

        TagModelDatabaseHelper(Context context, String databaseName, String tableName) {
            super(context, databaseName, null, VERSION);
            this.tableName = tableName;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = new StringBuilder().
            append("CREATE TABLE ").append(tableName).append(" (").
                append(AbstractController.KEY_ROWID).append(" integer primary key autoincrement, ").
                append(NAME).append(" text unique not null,").
                append(NOTES).append(" text not null,").
                append(ICON).append(" integer,").
                append(PARENT).append(" integer,").
                append(FLAGS).append(" integer not null,").
                append(LOCATION_LAT).append(" integer,").
                append(LOCATION_LONG).append(" integer,").
                append(NOTIFICATIONS).append(" integer,").
                append(CREATION_DATE).append(" integer").
            append(");").toString();
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(getClass().getSimpleName(), "Upgrading database from version " +
                    oldVersion + " to " + newVersion + ".");

            switch(oldVersion) {
            default:
                // we don't know how to handle it... do the unfortunate thing
                Log.e(getClass().getSimpleName(), "Unsupported migration, table dropped!");
                db.execSQL("DROP TABLE IF EXISTS " + tableName);
                onCreate(db);
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

        Integer id = retrieveInteger(AbstractController.KEY_ROWID);
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
        setValues.put(NAME, name);
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
