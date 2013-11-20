package com.todoroo.astrid.data;

import android.content.ContentValues;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;

public class TagMetadata extends AbstractModel {

    public static final Table TABLE = new Table("tag_metadata", TagMetadata.class);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Tag local id */
    public static final LongProperty TAG_ID = new LongProperty(
            TABLE, "tag_id");

    /** Tag uuid */
    public static final StringProperty TAG_UUID = new StringProperty(
            TABLE, "tag_uuid");

    /** Metadata Key */
    public static final StringProperty KEY = new StringProperty(
            TABLE, "key");

    /** Metadata Text Value Column 1 */
    public static final StringProperty VALUE1 = new StringProperty(
            TABLE, "value");

    /** Unixtime metadata was deleted/tombstoned */
    public static final LongProperty DELETION_DATE = new LongProperty(
            TABLE, "deleted");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(TagMetadata.class);


    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(DELETION_DATE.name, 0L);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    public TagMetadata() {
        super();
    }

    public TagMetadata(TodorooCursor<TagMetadata> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<TagMetadata> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    // --- parcelable helpers

    private static final Creator<TagMetadata> CREATOR = new ModelCreator<>(TagMetadata.class);
}
