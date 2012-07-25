/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entry for a task alarm
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AlarmFields {

    /** metadata key */
    public static final String METADATA_KEY = "alarm"; //$NON-NLS-1$

    /** time of alarm */
    public static final LongProperty TIME = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** alarm type */
    public static final IntegerProperty TYPE = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    // --- constants

    /** this alarm is single-shot */
    public static final int TYPE_SINGLE = 1;

    /** this alarm repeats itself until turned off */
    public static final int TYPE_REPEATING = 2;

}
