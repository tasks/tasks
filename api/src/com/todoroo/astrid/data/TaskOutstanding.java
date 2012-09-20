package com.todoroo.astrid.data;

import android.content.ContentValues;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;

@SuppressWarnings("nls")
public class TaskOutstanding extends OutstandingEntry<Task> {

    /** table for this model */
    public static final Table TABLE = new Table("tasks_outstanding", TaskOutstanding.class);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    public static final LongProperty TASK_ID = new LongProperty(
            TABLE, ENTITY_ID_PROPERTY_NAME);

    public static final StringProperty COLUMN_STRING = new StringProperty(
            TABLE, COLUMN_STRING_PROPERTY_NAME);

    public static final StringProperty VALUE_STRING = new StringProperty(
            TABLE, VALUE_STRING_PROPERTY_NAME);

    public static final LongProperty CREATED_AT = new LongProperty(
            TABLE, CREATED_AT_PROPERTY_NAME);

    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(TASK_ID.name, 0);
        defaultValues.put(COLUMN_STRING.name, "");
        defaultValues.put(VALUE_STRING.name, "");
    }

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(TaskOutstanding.class);

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    public static final Creator<TaskOutstanding> CREATOR = new ModelCreator<TaskOutstanding>(TaskOutstanding.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
