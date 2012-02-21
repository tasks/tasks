package com.todoroo.astrid.subtasks;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entries for a GTasks Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SubtasksMetadata {

    public static final long LIST_ACTIVE_TASKS = 0;

    /** metadata key */
    public static final String METADATA_KEY = "subtasks"; //$NON-NLS-1$

    /** list id */
    public static final LongProperty LIST_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    public static final IntegerProperty INDENT = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    public static final LongProperty ORDER = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

}
