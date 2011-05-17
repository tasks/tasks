package com.todoroo.astrid.actfm;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entry for a task shared with astrid.com
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskFields {

    /** metadata key */
    public static final String METADATA_KEY = "actfm"; //$NON-NLS-1$

    /** remote id*/
    public static final LongProperty REMOTE_ID = new LongProperty(Metadata.TABLE, Metadata.VALUE2.name);

    /** goal id */
    public static final LongProperty GOAL_ID = new LongProperty(Metadata.TABLE, Metadata.VALUE2.name);

    /** user id */
    public static final LongProperty USER_ID = new LongProperty(Metadata.TABLE, Metadata.VALUE3.name);

    /** user */
    public static final StringProperty USER = Metadata.VALUE4;

    /** comment count */
    public static final IntegerProperty COMMENT_COUNT = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

}
