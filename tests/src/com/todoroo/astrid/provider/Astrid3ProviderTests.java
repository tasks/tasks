/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

public class Astrid3ProviderTests extends DatabaseTestCase {

    String[] PROJECTION = new String[] {
            Task.ID.name,
            Task.TITLE.name,
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // set up database
        Astrid3ContentProvider.setDatabaseOverride(database);

    }

    /** Test CRUD over tasks with the ALL ITEMS cursor */
    public void testAllItemsCrud() {
        ContentResolver resolver = getContext().getContentResolver();

        // fetch all tasks, get nothing
        Uri uri = Task.CONTENT_URI;
        Cursor cursor = resolver.query(uri, PROJECTION, "1", null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // insert a task
        ContentValues values = new ContentValues();
        values.put(Task.TITLE.name, "mf doom?");
        resolver.insert(uri, values);

        // fetch all tasks, get something
        cursor = resolver.query(uri, PROJECTION, "1", null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("mf doom?", cursor.getString(1));
        cursor.close();

        // update all tasks
        values.put(Task.TITLE.name, "mf grimm?");
        resolver.update(uri, values, "1", null);

        // fetch all tasks, get something
        cursor = resolver.query(uri, PROJECTION, "1", null, null);
        cursor.moveToFirst();
        assertEquals("mf grimm?", cursor.getString(1));
        cursor.close();

        // delete a task
        assertEquals(1, resolver.delete(uri, "1", null));

        // fetch all tasks, get nothing
        cursor = resolver.query(uri, PROJECTION, null, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    /** Test selecting data */
    public void testSelection() {
        ContentResolver resolver = getContext().getContentResolver();
        Uri uri = Task.CONTENT_URI;

        // insert some tasks
        ContentValues values = new ContentValues();
        values.put(Task.TITLE.name, "tujiko noriko");
        values.put(Task.IMPORTANCE.name, Task.IMPORTANCE_MUST_DO);
        resolver.insert(uri, values);

        values.clear();
        values.put(Task.TITLE.name, "miho asahi");
        values.put(Task.IMPORTANCE.name, Task.IMPORTANCE_NONE);
        resolver.insert(uri, values);

        // fetch all tasks with various selection parameters
        Cursor cursor = resolver.query(uri, PROJECTION, "1", null, null);
        assertEquals(2, cursor.getCount());
        cursor.close();

        cursor = resolver.query(uri, PROJECTION, null, null, null);
        assertEquals(2, cursor.getCount());
        cursor.close();

        cursor = resolver.query(uri, PROJECTION, Task.IMPORTANCE + "=" +
                Task.IMPORTANCE_MUST_DO, null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("tujiko noriko", cursor.getString(1));
        cursor.close();

        cursor = resolver.query(uri, PROJECTION, Task.IMPORTANCE + ">" +
                Task.IMPORTANCE_MUST_DO, null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("miho asahi", cursor.getString(1));
        cursor.close();

        cursor = resolver.query(uri, PROJECTION, Task.IMPORTANCE + "=" +
                Task.IMPORTANCE_DO_OR_DIE, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    /** Test updating */
    public void testUpdating() {
        ContentResolver resolver = getContext().getContentResolver();

        // insert some tasks
        ContentValues values = new ContentValues();
        values.put(Task.TITLE.name, "carlos silva");
        values.put(Task.IMPORTANCE.name, Task.IMPORTANCE_SHOULD_DO);

        Uri carlosUri = resolver.insert(Task.CONTENT_URI, values);

        values.clear();
        values.put(Task.TITLE.name, "felix hernandez");
        values.put(Task.IMPORTANCE.name, Task.IMPORTANCE_MUST_DO);
        resolver.insert(Task.CONTENT_URI, values);

        String[] projection = new String[] {
                Task.ID.name,
                Task.TITLE.name,
                Task.IMPORTANCE.name,
        };

        // test updating with single item URI
        Cursor cursor = resolver.query(Task.CONTENT_URI, projection,
                Task.TITLE.eq("carlos who?").toString(), null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        values.clear();
        values.put(Task.TITLE.name, "carlos who?");
        assertEquals(1, resolver.update(carlosUri, values, null, null));

        cursor = resolver.query(Task.CONTENT_URI, projection, Task.TITLE.eq("carlos who?").toString(), null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();

        // test updating with all items uri
        cursor = resolver.query(Task.CONTENT_URI, PROJECTION,
                Task.IMPORTANCE.eq(Task.IMPORTANCE_NONE).toString(), null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        values.clear();
        values.put(Task.IMPORTANCE.name, Task.IMPORTANCE_NONE);
        assertEquals(1, resolver.update(Task.CONTENT_URI, values,
                Task.IMPORTANCE.eq(Task.IMPORTANCE_SHOULD_DO).toString(), null));

        cursor = resolver.query(Task.CONTENT_URI, PROJECTION,
                Task.IMPORTANCE.eq(Task.IMPORTANCE_NONE).toString(), null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();

        // test updating with group by uri
        try {
            Uri groupByUri = Uri.withAppendedPath(Task.CONTENT_URI,
                    AstridApiConstants.GROUP_BY_URI + Task.TITLE.name);
            resolver.update(groupByUri, values, null, null);
            fail("Able to update using groupby uri");
        } catch (Exception e) {
            // expected
        }
    }

    /** Test deleting */
    public void testDeleting() {
        ContentResolver resolver = getContext().getContentResolver();
        Uri allItemsUri = Task.CONTENT_URI;

        // insert some tasks
        ContentValues values = new ContentValues();
        values.put(Task.TITLE.name, "modest mouse");
        values.put(Task.IMPORTANCE.name, Task.IMPORTANCE_DO_OR_DIE);
        Uri modestMouse = resolver.insert(allItemsUri, values);

        values.clear();
        values.put(Task.TITLE.name, "death cab");
        values.put(Task.IMPORTANCE.name, Task.IMPORTANCE_MUST_DO);
        resolver.insert(allItemsUri, values);

        values.clear();
        values.put(Task.TITLE.name, "murder city devils");
        values.put(Task.IMPORTANCE.name, Task.IMPORTANCE_SHOULD_DO);
        resolver.insert(allItemsUri, values);

        // test deleting with single URI
        Cursor cursor = resolver.query(allItemsUri, PROJECTION, Task.TITLE.name +
                " = 'modest mouse'", null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();

        assertEquals(1, resolver.delete(modestMouse, null, null));

        cursor = resolver.query(allItemsUri, PROJECTION, Task.TITLE.name +
                " = 'modest mouse'", null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // test updating with all items uri
        cursor = resolver.query(allItemsUri, PROJECTION, Task.TITLE.name +
                " = 'murder city devils'", null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();

        assertEquals(1, resolver.delete(allItemsUri, Task.IMPORTANCE.name +
                ">" + Task.IMPORTANCE_MUST_DO, null));

        cursor = resolver.query(allItemsUri, PROJECTION, Task.TITLE.name +
                " = 'murder city devils'", null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // test with group by uri
        try {
            Uri groupByUri = Uri.withAppendedPath(Task.CONTENT_URI,
                    AstridApiConstants.GROUP_BY_URI + Task.TITLE.name);
            resolver.delete(groupByUri, null, null);
            fail("Able to delete using groupby uri");
        } catch (Exception e) {
            // expected
        }
    }

    /** Test CRUD over SINGLE ITEM uri */
    public void testSingleItemCrud() {
        ContentResolver resolver = getContext().getContentResolver();

        Uri uri = StoreObject.CONTENT_URI;

        ContentValues values = new ContentValues();
        values.put(StoreObject.TYPE.name, "rapper");
        values.put(StoreObject.ITEM.name, "mf doom?");
        Uri firstUri = resolver.insert(uri, values);

        values.put(StoreObject.ITEM.name, "gm grimm!");
        Uri secondUri = resolver.insert(uri, values);
        assertNotSame(firstUri, secondUri);

        String[] storeProjection = new String[] {
                StoreObject.ITEM.name,
        };


        Cursor cursor = resolver.query(uri, storeProjection, null, null, null);
        assertEquals(2, cursor.getCount());
        cursor.close();

        cursor = resolver.query(firstUri, storeProjection, null, null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("mf doom?", cursor.getString(0));
        cursor.close();

        values.put(StoreObject.ITEM.name, "danger mouse.");
        resolver.update(firstUri, values, null, null);
        cursor = resolver.query(firstUri, storeProjection, null, null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("danger mouse.", cursor.getString(0));
        cursor.close();

        assertEquals(1, resolver.delete(firstUri, null, null));

        cursor = resolver.query(uri, storeProjection, null, null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();
    }

    /** Test GROUP BY uri */
    public void testGroupByCrud() {
        ContentResolver resolver = getContext().getContentResolver();
        Uri uri = Task.CONTENT_URI;

        ContentValues values = new ContentValues();
        values.put(Task.TITLE.name, "catwoman");
        resolver.insert(uri, values);

        values.put(Task.TITLE.name, "the joker");
        resolver.insert(uri, values);
        resolver.insert(uri, values);
        resolver.insert(uri, values);

        values.put(Task.TITLE.name, "deep freeze");
        resolver.insert(uri, values);
        resolver.insert(uri, values);

        Uri groupByUri = Uri.withAppendedPath(Task.CONTENT_URI,
                AstridApiConstants.GROUP_BY_URI + Task.TITLE.name);
        Cursor cursor = resolver.query(groupByUri, PROJECTION, null, null, Task.TITLE.name);
        assertEquals(3, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("catwoman", cursor.getString(1));
        cursor.moveToNext();
        assertEquals("deep freeze", cursor.getString(1));
        cursor.moveToNext();
        assertEquals("the joker", cursor.getString(1));
        cursor.close();

        // test "group-by" with metadata
        IntegerProperty age = new IntegerProperty(Metadata.TABLE, Metadata.VALUE1.name);
        StringProperty size = Metadata.VALUE2;

        uri = Metadata.CONTENT_URI;

        values.clear();
        values.put(Metadata.TASK.name, 1);
        values.put(Metadata.KEY.name, "sizes");
        values.put(age.name, 50);
        values.put(size.name, "large");
        resolver.insert(uri, values);

        values.put(age.name, 40);
        values.put(size.name, "large");
        resolver.insert(uri, values);

        values.put(age.name, 20);
        values.put(size.name, "small");
        resolver.insert(uri, values);

        String[] metadataProjection = new String[] { "AVG(" + age + ")" };

        Uri groupBySizeUri = Uri.withAppendedPath(Metadata.CONTENT_URI,
                AstridApiConstants.GROUP_BY_URI + size.name);
        cursor = resolver.query(groupBySizeUri, metadataProjection, null, null, size.name);
        assertEquals(2, cursor.getCount());

        cursor.moveToFirst();
        assertEquals(45, cursor.getInt(0));
        cursor.moveToNext();
        assertEquals(20, cursor.getInt(0));
    }

}
