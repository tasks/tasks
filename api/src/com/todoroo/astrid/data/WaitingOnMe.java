package com.todoroo.astrid.data;

import android.content.ContentValues;
import android.net.Uri;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.api.AstridApiConstants;

@SuppressWarnings("nls")
public class WaitingOnMe extends RemoteModel {

    /** table for this model */
    public static final Table TABLE = new Table("waitingOnMe", WaitingOnMe.class);

    /** model class for entries in the outstanding table */
    public static final Class<? extends OutstandingEntry<WaitingOnMe>> OUTSTANDING_MODEL = WaitingOnMeOutstanding.class;

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.API_PACKAGE + "/" +
            TABLE.name);

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Remote ID */
    public static final StringProperty UUID = new StringProperty(
            TABLE, RemoteModel.UUID_PROPERTY_NAME);

    public static final StringProperty WAITING_USER_ID = new StringProperty(
            TABLE, "waiting_user_id", Property.PROP_FLAG_USER_ID);

    public static final StringProperty TASK_UUID = new StringProperty(
            TABLE, "task_uuid");

    public static final StringProperty WAIT_TYPE = new StringProperty(
            TABLE, "wait_type");

    public static final LongProperty CREATED_AT = new LongProperty(
            TABLE, "created_at", Property.PROP_FLAG_DATE);

    public static final LongProperty DELETED_AT = new LongProperty(
            TABLE, "deleted_at", Property.PROP_FLAG_DATE);

    public static final LongProperty READ_AT = new LongProperty(
            TABLE, "read_at", Property.PROP_FLAG_DATE);

    public static final IntegerProperty ACKNOWLEDGED = new IntegerProperty(
            TABLE, "acknowledged", Property.PROP_FLAG_BOOLEAN);

    public static final LongProperty PUSHED_AT = new LongProperty(
            TABLE, PUSHED_AT_PROPERTY_NAME, Property.PROP_FLAG_DATE);

    @Override
    public String getUuid() {
        return getUuidHelper(UUID);
    }

    public WaitingOnMe() {
        super();
    }

    public WaitingOnMe(TodorooCursor<WaitingOnMe> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<WaitingOnMe> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(UUID.name, RemoteModel.NO_UUID);
        defaultValues.put(WAITING_USER_ID.name, RemoteModel.NO_UUID);
        defaultValues.put(TASK_UUID.name, RemoteModel.NO_UUID);
        defaultValues.put(CREATED_AT.name, 0L);
        defaultValues.put(DELETED_AT.name, 0L);
        defaultValues.put(READ_AT.name, 0L);
    }

    public static final String WAIT_TYPE_COMMENTED = "commented";
    public static final String WAIT_TYPE_ASSIGNED = "assigned";
    public static final String WAIT_TYPE_MENTIONED = "mentioned";
    public static final String WAIT_TYPE_RAISED_PRI = "raised_pri";
    public static final String WAIT_TYPE_CHANGED_DUE = "changed_due";

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    public static final Property<?>[] PROPERTIES = generateProperties(WaitingOnMe.class);

    private static final Creator<WaitingOnMe> CREATOR = new ModelCreator<WaitingOnMe>(WaitingOnMe.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
