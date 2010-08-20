package com.todoroo.astrid.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

public class Astrid3ProviderTests extends DatabaseTestCase {

    String[] PROJECTION = new String[] {
            Task.ID.name,
            Task.TITLE.name,
    };

    /** Test CRUD over tasks with the ALL ITEMS cursor */
    public void testAllItemsCrud() {
        ContentResolver resolver = getContext().getContentResolver();

        // fetch all tasks, get nothing
        Uri uri = Uri.withAppendedPath(Task.CONTENT_URI, "");
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

        cursor = resolver.query(uri, PROJECTION, Task.IMPORTANCE + "<" +
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

    /** Test selecting data with metadata */
    public void testSelectionWithMetadata() {
        ContentResolver resolver = getContext().getContentResolver();

        // insert some tasks
        ContentValues task = new ContentValues();
        ContentValues metadata = new ContentValues();
        task.put(Task.TITLE.name, "turkoglu");
        metadata.put("suave-factor", "10");
        resolver.insert(Task.CONTENT_URI, task);
        resolver.insert(Metadata.CONTENT_URI, metadata);

        task.put(Task.TITLE.name, "ichiro");
        metadata.put("suave-factor", "30");
        resolver.insert(Task.CONTENT_URI, task);
        resolver.insert(Metadata.CONTENT_URI, metadata);

        task.put(Task.TITLE.name, "oprah");
        metadata.put("suave-factor", "-10");
        resolver.insert(Task.CONTENT_URI, task);
        resolver.insert(Metadata.CONTENT_URI, metadata);

        task.put(Task.TITLE.name, "cruise");
        metadata.put("suave-factor", "-10");
        resolver.insert(Task.CONTENT_URI, task);
        resolver.insert(Metadata.CONTENT_URI, metadata);

        task.put(Task.TITLE.name, "cruise");
        metadata.put("suave-factor", "-10");
        resolver.insert(Task.CONTENT_URI, task);
        resolver.insert(Metadata.CONTENT_URI, metadata);

        task.clear();
        task.put(Task.TITLE.name, "oprah");
        resolver.insert(uri, task);

        String[] projection = new String[] {
                Task.ID,
                Task.TITLE.name,
                "suave-factor"
        };

        // fetch all tasks with various selection parameters
        Cursor cursor = resolver.query(uri, projection, "1", null, null);
        assertEquals(3, cursor.getCount());
        cursor.close();

        cursor = resolver.query(uri, projection, "'suave-factor' = 30", null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("ichiro", cursor.getString(1));
        cursor.close();

        cursor = resolver.query(uri, projection, "'suave-factor' < 5", null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("cruise", cursor.getString(1));
        cursor.close();

        cursor = resolver.query(uri, projection, "'suave-factor' ISNULL", null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("oprah", cursor.getString(1));
        cursor.close();
    }

    /** Test updating */
    public void testUpdating() {
        ContentResolver resolver = getContext().getContentResolver();
        Uri allItemsUri = AstridContentProvider.allItemsUri();

        // insert some tasks
        ContentValues values = new ContentValues();
        values.put(Task.TITLE.name, "carlos silva");
        values.put("suckitude", "acute");
        Uri carlosUri = resolver.insert(allItemsUri, values);

        values.clear();
        values.put(Task.TITLE.name, "felix hernandez");
        values.put(Task.URGENCY, Task.URGENCY_WITHIN_A_YEAR);
        resolver.insert(allItemsUri, values);

        String[] projection = new String[] {
                Task.ID,
                Task.TITLE.name,
                "suckitude"
        };

        // test updating with single item URI
        Cursor cursor = resolver.query(allItemsUri, projection, "'suckitude' = 'carlos who?'", null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        values.clear();
        values.put("suckitude", "carlos who?");
        assertEquals(1, resolver.update(carlosUri, values, null, null));

        cursor = resolver.query(allItemsUri, projection, "'suckitude' = 'carlos who?'", null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();

        // test updating with all items uri
        cursor = resolver.query(allItemsUri, PROJECTION, Task.URGENCY + " = " +
                Task.URGENCY_NONE, null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        values.clear();
        values.put(Task.URGENCY, Task.URGENCY_NONE);
        assertEquals(1, resolver.update(allItemsUri, values, Task.URGENCY +
                "=" + Task.URGENCY_WITHIN_A_YEAR, null));

        cursor = resolver.query(allItemsUri, PROJECTION, Task.URGENCY + " = " +
                Task.URGENCY_NONE, null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();

        // test updating with group by uri
        try {
            Uri groupByUri = AstridContentProvider.groupByUri(Task.TITLE.name);
            resolver.update(groupByUri, values, null, null);
            fail("Able to update using groupby uri");
        } catch (Exception e) {
            // expected
        }
    }

    /** Test deleting */
    public void testDeleting() {
        ContentResolver resolver = getContext().getContentResolver();
        Uri allItemsUri = AstridContentProvider.allItemsUri();

        // insert some tasks
        ContentValues values = new ContentValues();
        values.put(Task.TITLE.name, "modest mouse");
        values.put(Task.IMPORTANCE, Task.IMPORTANCE_DO_OR_DIE);
        Uri modestMouse = resolver.insert(allItemsUri, values);

        values.clear();
        values.put(Task.TITLE.name, "death cab");
        values.put(Task.IMPORTANCE, Task.IMPORTANCE_MUST_DO);
        resolver.insert(allItemsUri, values);

        values.clear();
        values.put(Task.TITLE.name, "murder city devils");
        values.put(Task.IMPORTANCE, Task.IMPORTANCE_SHOULD_DO);
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

        assertEquals(1, resolver.delete(allItemsUri, Task.IMPORTANCE +
                "<" + Task.IMPORTANCE_MUST_DO, null));

        cursor = resolver.query(allItemsUri, PROJECTION, Task.TITLE.name +
                " = 'murder city devils'", null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // test with group by uri
        try {
            Uri groupByUri = AstridContentProvider.groupByUri(Task.TITLE.name);
            resolver.delete(groupByUri, null, null);
            fail("Able to delete using groupby uri");
        } catch (Exception e) {
            // expected
        }
    }

    /** Test CRUD over SINGLE ITEM uri */
    public void testSingleItemCrud() {
        ContentResolver resolver = getContext().getContentResolver();

        Uri uri = AstridContentProvider.allItemsUri();

        ContentValues values = new ContentValues();
        values.put(Task.TITLE.name, "mf doom?");
        Uri firstUri = resolver.insert(uri, values);

        values.put(Task.TITLE.name, "gm grimm!");
        Uri secondUri = resolver.insert(uri, values);
        assertNotSame(firstUri, secondUri);

        Cursor cursor = resolver.query(uri, PROJECTION, "1", null, null);
        assertEquals(2, cursor.getCount());
        cursor.close();

        cursor = resolver.query(firstUri, PROJECTION, null, null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("mf doom?", cursor.getString(1));
        cursor.close();

        values.put(Task.TITLE.name, "danger mouse.");
        resolver.update(firstUri, values, null, null);
        cursor = resolver.query(firstUri, PROJECTION, null, null, null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("danger mouse.", cursor.getString(1));
        cursor.close();

        assertEquals(1, resolver.delete(firstUri, null, null));

        cursor = resolver.query(uri, PROJECTION, null, null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();
    }

    /** Test GROUP BY uri */
    public void testGroupByCrud() {
        ContentResolver resolver = getContext().getContentResolver();
        Uri uri = AstridContentProvider.allItemsUri();

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

        Uri groupByUri = AstridContentProvider.groupByUri(Task.TITLE.name);
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
        values.put("age", 50);
        values.put("size", "large");
        resolver.insert(uri, values);
        values.put("age", 40);
        values.put("size", "large");
        resolver.insert(uri, values);

        values.put("age", 20);
        values.put("size", "small");
        resolver.insert(uri, values);

        Uri groupByAgeUri = AstridContentProvider.groupByUri("size");
        cursor = resolver.query(groupByUri, PROJECTION, null, null, Task.TITLE.name);
        assertEquals(3, cursor.getCount());
    }

    /** Test updating and deleting with metadata */
    public void testMetadataUpdateDelete() {
        ContentResolver resolver = getContext().getContentResolver();
        Uri allItemsUri = AstridContentProvider.allItemsUri();

        // insert some tasks
        ContentValues values = new ContentValues();
        values.put(Task.TITLE.name, "chicago");
        values.put("pizza", "delicious");
        values.put("temperature", 20);
        resolver.insert(allItemsUri, values);

        values.clear();
        values.put(Task.TITLE.name, "san francisco");
        values.put("pizza", "meh");
        values.put("temperature", 60);
        resolver.insert(allItemsUri, values);

        values.clear();
        values.put(Task.TITLE.name, "new york");
        values.put("pizza", "yum");
        values.put("temperature", 30);
        resolver.insert(allItemsUri, values);

        // test updating with standard URI (shouldn't work)
        values.clear();
        values.put("pizza", "nonexistent, the city is underwater");
        assertEquals(0, resolver.update(allItemsUri, values,
                "'pizza'='yum'", null));

        String[] projection = new String[] {
                Task.ID,
                Task.TITLE.name,
                "pizza",
                "temperature"
        };

        Cursor cursor = resolver.query(allItemsUri, projection, "'pizza' = 'yum'", null, null);
        assertEquals(1, cursor.getCount());
        cursor.close();

        // test updating with metadata uri
        Uri pizzaUri = AstridContentProvider.allItemsWithMetadataUri(new String[] {"pizza"});
        assertEquals(1, resolver.update(pizzaUri, values,
                "'pizza'='yum'", null));

        cursor = resolver.query(allItemsUri, projection, "'pizza' = 'yum'", null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // test deleting with metadata uri
        Uri pizzaTempUri = AstridContentProvider.allItemsWithMetadataUri(new String[] {"pizza",
                "temperature"});

        cursor = resolver.query(allItemsUri, projection, Task.TITLE.name + " = 'chicago'", null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();

        // SQLITE: the deliverer of BAD NEWS
        assertEquals(0, resolver.delete(pizzaTempUri, "pizza='delicious' AND temperature > 50", null));
        assertEquals(1, resolver.delete(pizzaTempUri, "pizza='delicious' AND temperature < 50", null));

        cursor = resolver.query(allItemsUri, projection, Task.TITLE.name + " = 'chicago'", null, null);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }
}
