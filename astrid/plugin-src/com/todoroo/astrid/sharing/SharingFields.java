package com.todoroo.astrid.sharing;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entry for a task alarm
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SharingFields {

    /** metadata key */
    public static final String METADATA_KEY = "sharing"; //$NON-NLS-1$

    /** online url */
    public static final StringProperty URL = Metadata.VALUE1;

    /** sharing privacy */
    public static final IntegerProperty PRIVACY = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    // --- constants

    /** this task is shared publicly */
    public static final int PRIVACY_PUBLIC = 2;

    /** this task is shared with a limited group */
    public static final int PRIVACY_LIMITED = 2;

    /** this task is private */
    public static final int PRIVACY_PRIVATE = 1;

    /** this alarm repeats itself until turned off */
    public static final int TYPE_REPEATING = 2;

}
