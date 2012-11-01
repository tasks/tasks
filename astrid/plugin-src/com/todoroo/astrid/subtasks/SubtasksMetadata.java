/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.subtasks;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entries for a Subtask list
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Deprecated
public class SubtasksMetadata {

    public static final String LIST_ACTIVE_TASKS = "[AT]"; //$NON-NLS-1$

    /** metadata key */
    public static final String METADATA_KEY = "subtasks"; //$NON-NLS-1$

    /** tag name */
    public static final StringProperty TAG = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    public static final IntegerProperty INDENT = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    public static final LongProperty ORDER = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

}
