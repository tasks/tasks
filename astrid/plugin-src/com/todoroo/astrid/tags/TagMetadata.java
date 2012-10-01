package com.todoroo.astrid.tags;

import java.math.BigInteger;

import com.todoroo.andlib.data.Property.BigIntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

public class TagMetadata {

    /** Metadata key for tag data */
    public static final String KEY = "tags-tag"; //$NON-NLS-1$

    /** Property for reading tag values */
    public static final StringProperty TAG_NAME = Metadata.VALUE1;

    /** Tag uuid */
    public static final BigIntegerProperty TAG_UUID = new BigIntegerProperty(
            Metadata.TABLE, Metadata.VALUE2.name);

    /** Task uuid */
    public static final BigIntegerProperty TASK_UUID = new BigIntegerProperty(
            Metadata.TABLE, Metadata.VALUE3.name);

    /** Pushed at time */
    public static final LongProperty PUSHED_AT = new LongProperty(
            Metadata.TABLE, Metadata.VALUE4.name);


    // Creation date and deletion date are already included as part of the normal metadata entity

    /**
     * New metadata object for linking a task to the specified tag. The task
     * object should be saved and have the remote_id property. All parameters
     * are manditory
     * @param task
     * @param tagName
     * @param tagUuid
     * @return
     */
    public static Metadata newTagMetadata(Task task, String tagName, BigInteger tagUuid) {
        return newTagMetadata(task.getId(), task.getValue(Task.UUID), tagName, tagUuid);
    }

    public static Metadata newTagMetadata(long taskId, BigInteger taskUuid, String tagName, BigInteger tagUuid) {
        Metadata link = new Metadata();
        link.setValue(Metadata.KEY, KEY);
        link.setValue(Metadata.TASK, taskId);
        link.setValue(TAG_NAME, tagName);
        link.setValue(TASK_UUID, taskUuid);
        link.setValue(TAG_UUID, tagUuid);
        return link;
    }
}
