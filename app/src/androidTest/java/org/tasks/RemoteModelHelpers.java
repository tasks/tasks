package org.tasks;

import android.annotation.SuppressLint;
import android.content.ContentValues;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.astrid.data.RemoteModel;

import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

@SuppressLint("NewApi")
public class RemoteModelHelpers {
    public static Property[] asQueryProperties(Table table, ContentValues contentValues) {
        Set<String> keys = contentValues.keySet();
        Property[] result = new Property[keys.size()];
        int index = 0;
        for (String key : keys) {
            result[index++] = new Property.StringProperty(table, key);
        }
        return result;
    }

    public static void compareRemoteModel(RemoteModel expected, RemoteModel actual) {
        compareContentValues(expected.getSetValues(), actual.getSetValues());
        compareContentValues(expected.getDatabaseValues(), actual.getDatabaseValues());
    }

    private static void compareContentValues(ContentValues expected, ContentValues actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null) {
            fail();
        }
        for (String key : expected.keySet()) {
            Object entry = expected.get(key);
            if (entry instanceof Integer) {
                assertEquals(entry, actual.getAsInteger(key));
            } else if (entry instanceof String) {
                assertEquals(entry, actual.getAsString(key));
            } else if (entry instanceof Long) {
                assertEquals(entry, actual.getAsLong(key));
            } else {
                fail("Unhandled property type: " + entry.getClass());
            }
        }
    }

}
