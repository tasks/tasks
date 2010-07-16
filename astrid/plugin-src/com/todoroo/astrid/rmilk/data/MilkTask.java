package com.todoroo.astrid.rmilk.data;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.astrid.model.Metadata;

/**
 * Metadata entries for a Remember The Milk Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkTask {

    /** metadata key */
    public static final String METADATA_KEY = "rmilk"; //$NON-NLS-1$

    /** {@link MilkList} id */
    public static final LongProperty LIST_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** RTM Task Series Id */
    public static final LongProperty TASK_SERIES_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** RTM Task Id */
    public static final LongProperty TASK_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    /** Whether task repeats in RTM (1 or 0) */
    public static final IntegerProperty REPEATING = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

}
