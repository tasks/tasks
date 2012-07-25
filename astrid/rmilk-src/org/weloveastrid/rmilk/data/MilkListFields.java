/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.rmilk.data;


import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.StoreObject;

/**
 * Data Model which represents a list in RTM
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class MilkListFields {

    /** type*/
    public static final String TYPE = "rmilk-list"; //$NON-NLS-1$

    // --- properties

    /** Remote ID */
    public static final LongProperty REMOTE_ID = new LongProperty(
            StoreObject.TABLE, StoreObject.ITEM.name);

    /** Name */
    public static final StringProperty NAME = StoreObject.VALUE1;

    /** Position */
    public static final IntegerProperty POSITION = new IntegerProperty(
            StoreObject.TABLE, StoreObject.VALUE2.name);

    /** Archived (0 or 1) */
    public static final IntegerProperty ARCHIVED = new IntegerProperty(
            StoreObject.TABLE, StoreObject.VALUE3.name);

}
