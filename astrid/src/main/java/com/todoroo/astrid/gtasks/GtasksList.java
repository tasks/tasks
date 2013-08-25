/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.StoreObject;

/**
 * {@link StoreObject} entries for a GTasks List
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksList {

    /** type*/
    public static final String TYPE = "gtasks-list"; //$NON-NLS-1$

    /** list id in g-tasks */
    public static final StringProperty REMOTE_ID = new StringProperty(StoreObject.TABLE,
            StoreObject.ITEM.name);

    /** list name */
    public static final StringProperty NAME = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE1.name);

    /** list order */
    public static final IntegerProperty ORDER = new IntegerProperty(StoreObject.TABLE,
            StoreObject.VALUE2.name);

    public static final LongProperty LAST_SYNC = new LongProperty(StoreObject.TABLE,
            StoreObject.VALUE3.name);

}
