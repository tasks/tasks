/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.rmilk.data;


import android.content.ContentValues;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.astrid.model.Task;

/**
 * Data Model which represents a list in RTM
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class MilkTask extends AbstractModel {

    // --- table

    public static final Table TABLE = new Table("tasks", MilkTask.class);

    // --- properties

    /** Task Id */
    public static final LongProperty TASK = new LongProperty(
            TABLE, "task");

    /** {@link MilkList} id */
    public static final LongProperty LIST_ID = new LongProperty(
            TABLE, "listId");

    /** RTM Task Id */
    public static final LongProperty TASK_SERIES_ID = new LongProperty(
            TABLE, "taskSeriesId");

    /** RTM Task Series Id */
    public static final LongProperty TASK_ID = new LongProperty(
            TABLE, "taskId");

    /** Whether task repeats in RTM (1 or 0) */
    public static final IntegerProperty REPEATING = new IntegerProperty(
            TABLE, "repeating");

    /** Unixtime task was last updated in RTM */
    public static final LongProperty UPDATED = new LongProperty(
            TABLE, "updated");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(MilkTask.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(REPEATING.name, 0);
        defaultValues.put(UPDATED.name, 0);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- data access boilerplate

    public MilkTask() {
        super();
    }

    public MilkTask(TodorooCursor<MilkTask> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<MilkTask> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(TASK);
    };


    // --- parcelable helpers

    private static final Creator<Task> CREATOR = new ModelCreator<Task>(Task.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
