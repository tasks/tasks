package com.todoroo.astrid.taskrabbit;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entries for a GTasks Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskRabbitMetadata {

    static final int VALUE_UNSET = -1;

    /** metadata key */
    public static final String METADATA_KEY = "taskrabbit"; //$NON-NLS-1$

    /** task id in taskrabbit */
    public static final StringProperty ID = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    public static final StringProperty DATA_LOCAL = new StringProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    public static final StringProperty DATA_REMOTE = new StringProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    /**
     * Creates default GTasks metadata item
     * @param taskId if > 0, will set metadata task field
     * @return
     */
    public static Metadata createEmptyMetadata(long taskId) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, TaskRabbitMetadata.METADATA_KEY);
        metadata.setValue(ID, ""); //$NON-NLS-1$
        metadata.setValue(DATA_LOCAL, ""); //$NON-NLS-1$
        metadata.setValue(DATA_REMOTE, ""); //$NON-NLS-1$
        if(taskId > AbstractModel.NO_ID)
            metadata.setValue(Metadata.TASK, taskId);
        return metadata;
    }

}
