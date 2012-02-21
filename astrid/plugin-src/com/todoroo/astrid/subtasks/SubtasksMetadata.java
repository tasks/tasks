package com.todoroo.astrid.subtasks;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entries for a GTasks Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SubtasksMetadata {

    static final int VALUE_UNSET = -1;

    /** metadata key */
    public static final String METADATA_KEY = "subtasks"; //$NON-NLS-1$

    /** list id */
    public static final StringProperty LIST_ID = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    public static final IntegerProperty INDENT = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    public static final LongProperty ORDER = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

}
