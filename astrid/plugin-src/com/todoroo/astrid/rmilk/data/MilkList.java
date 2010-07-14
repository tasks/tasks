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
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.model.Task;

/**
 * Data Model which represents a list in RTM
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class MilkList extends AbstractModel {

    // --- table

    public static final Table TABLE = new Table("lists", MilkList.class);

    // --- properties

    /** ID (corresponds to RTM ID) */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Name */
    public static final StringProperty NAME = new StringProperty(
            TABLE, "name");

    /** Position */
    public static final IntegerProperty POSITION = new IntegerProperty(
            TABLE, "position");

    /** Archived (0 or 1) */
    public static final IntegerProperty ARCHIVED = new IntegerProperty(
            TABLE, "archived");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(MilkList.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(POSITION.name, 0);
        defaultValues.put(ARCHIVED.name, 0);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- data access boilerplate

    public MilkList() {
        super();
    }

    public MilkList(TodorooCursor<MilkList> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<MilkList> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    };

    /**
     * @return whether this list is archived. requires {@link ARCHIVED}
     */
    public boolean isArchived() {
        return getValue(ARCHIVED) > 0;
    }

    // --- parcelable helpers

    private static final Creator<Task> CREATOR = new ModelCreator<Task>(Task.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
