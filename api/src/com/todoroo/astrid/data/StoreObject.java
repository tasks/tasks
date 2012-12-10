/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;


import android.content.ContentValues;
import android.net.Uri;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.api.AstridApiConstants;

/**
 * Data Model which represents a piece of data unrelated to a task
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class StoreObject extends AbstractModel {

    // --- table

    /** table for this model */
    public static final Table TABLE = new Table("store", StoreObject.class);

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.API_PACKAGE + "/" +
            TABLE.name);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Store Type Key */
    public static final StringProperty TYPE = new StringProperty(
            TABLE, "type");

    /** Store Item Key */
    public static final StringProperty ITEM= new StringProperty(
            TABLE, "item");

    /** Store Value Column 1 */
    public static final StringProperty VALUE1 = new StringProperty(
            TABLE, "value");

    /** Store Value Column 2 */
    public static final StringProperty VALUE2 = new StringProperty(
            TABLE, "value2");

    /** Store Value Column 3 */
    public static final StringProperty VALUE3 = new StringProperty(
            TABLE, "value3");

    /** Store Value Column 4 */
    public static final StringProperty VALUE4 = new StringProperty(
            TABLE, "value4");

    /** Store Value Column 5 */
    public static final StringProperty VALUE5 = new StringProperty(
            TABLE, "value5");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(StoreObject.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- data access boilerplate

    public StoreObject() {
        super();
    }

    public StoreObject(TodorooCursor<StoreObject> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<StoreObject> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    };

    // --- parcelable helpers

    private static final Creator<StoreObject> CREATOR = new ModelCreator<StoreObject>(StoreObject.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
