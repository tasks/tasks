/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev.sync;


import android.content.ContentValues;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.model.Task;

/**
 * Data Model which represents a dashboard in Producteev
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class ProducteevDashboard extends AbstractModel {

    // --- table

    public static final Table TABLE = new Table("dashboards", ProducteevDashboard.class);

    // --- properties

    /** ID (corresponds to RTM ID) */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Name */
    public static final StringProperty NAME = new StringProperty(
            TABLE, "name");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(ProducteevDashboard.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

//    static {
//        defaultValues.put(POSITION.name, 0);
//        defaultValues.put(ARCHIVED.name, 0);
//    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- data access boilerplate

    public ProducteevDashboard() {
        super();
    }

    public ProducteevDashboard(TodorooCursor<ProducteevDashboard> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<ProducteevDashboard> cursor) {
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
