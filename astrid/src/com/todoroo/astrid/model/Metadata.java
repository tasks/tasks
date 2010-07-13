/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.model;


import android.content.ContentValues;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;

/**
 * Data Model which represents a piece of metadata associated with a task
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class Metadata extends AbstractModel {

    // --- table

    public static final Table TABLE = new Table("metadata", Metadata.class);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Associated Task */
    public static final LongProperty TASK = new LongProperty(
            TABLE, "task");

    /** Metadata Key */
    public static final StringProperty KEY = new StringProperty(
            TABLE, "key");

    /** Metadata Text Value */
    public static final StringProperty VALUE = new StringProperty(
            TABLE, "value");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(Metadata.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- data access boilerplate

    public Metadata() {
        super();
    }

    public Metadata(TodorooCursor<Metadata> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<Metadata> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    };

    // --- parcelable helpers

    private static final Creator<Task> CREATOR = new ModelCreator<Task>(Task.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
