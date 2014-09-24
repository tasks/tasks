package com.todoroo.astrid.data;

import android.content.ContentValues;
import android.net.Uri;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.api.AstridApiConstants;

import java.util.Objects;

public class TaskTimeLog extends RemoteModel  {
    /** table for this model */
    public static final Table TABLE = new Table("taskTimeLog", TaskTimeLog.class);

    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.API_PACKAGE + "/" +
            TABLE.name);
    /** ID */
    public static final Property.LongProperty ID = new Property.LongProperty(
            TABLE, ID_PROPERTY_NAME);

    public static final Property.StringProperty UUID = new Property.StringProperty(
            TABLE, UUID_PROPERTY_NAME);

    public static final Property.LongProperty TIME_SPENT = new Property.LongProperty(
            TABLE, "timeSpent", Property.PROP_FLAG_DATE);
    public static final Property.LongProperty TIME = new Property.LongProperty(
            TABLE, "time", Property.PROP_FLAG_DATE);
    public static final Property.StringProperty DESCRIPTION = new Property.StringProperty(
            TABLE, "comment");
    /** Task local id */
    public static final Property.LongProperty TASK_ID = new Property.LongProperty(
            TABLE, "task_id");
    /** Task uuid */
    public static final Property.StringProperty TASK_UUID = new Property.StringProperty(
            TABLE, "task_uuid");

    private static final ContentValues defaultValues = new ContentValues();

    public static final Property<?>[] PROPERTIES = generateProperties(TaskTimeLog.class);

    static {
        defaultValues.put(TIME_SPENT.name, 0);
        defaultValues.put(TIME.name, 0);
        defaultValues.put(DESCRIPTION.name, "");
        defaultValues.put(TASK_UUID.name, NO_UUID);
        defaultValues.put(UUID.name, NO_UUID);
    }
    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }
    public TaskTimeLog() {
        super();
    }

    public TaskTimeLog(TodorooCursor<TaskTimeLog> cursor) {
        super(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    // --- parcelable helpers

    public static final Creator<TaskTimeLog> CREATOR = new ModelCreator<>(TaskTimeLog.class);


    public Long getID() {
        return getValue(ID);
    }
    public Long getTimeSpent() {
        return getValue(TIME_SPENT);
    }
    public Long getTime() {
        return getValue(TIME);
    }
    public String getDescription() {
        return getValue(DESCRIPTION);
    }
    public Long getTaskId() {
        return getValue(TASK_ID);
    }
    public String getTaskUuid() { return getValue(TASK_UUID); }
    public String getUuid() {
        return getValue(UUID);
    }

    public void setID(Long id) {setValue(ID, id);}
    public void setTimeSpent(Long timeSpent) {setValue(TIME_SPENT, timeSpent);}
    public void setTime(Long time) {setValue(TIME, time);}
    public void setDescription(String description) {
        setValue(DESCRIPTION, description);
    }
    public void setTaskId(Long taskId) {setValue(TASK_ID, taskId);}
    public void setTaskUuid(String taskUuid) {
        setValue(TASK_UUID, taskUuid);
    }
    public void setUuid(String uuid){
        setValue(UUID, uuid);
    }

}
