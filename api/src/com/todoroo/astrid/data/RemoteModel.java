package com.todoroo.astrid.data;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;

/**
 * A model that is synchronized to a remote server and has a remote id
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class RemoteModel extends AbstractModel {

    /** remote id property common to all remote models */
    public static final String REMOTE_ID_PROPERTY_NAME = "remoteId"; //$NON-NLS-1$

    /** remote id property */
    public static final LongProperty REMOTE_ID_PROPERTY = new LongProperty(null, REMOTE_ID_PROPERTY_NAME);

    /** user id property common to all remote models */
    protected static final String USER_ID_PROPERTY_NAME = "userId"; //$NON-NLS-1$

    /** user id property */
    public static final LongProperty USER_ID_PROPERTY = new LongProperty(null, USER_ID_PROPERTY_NAME);

    /** user json property common to all remote models */
    protected static final String USER_JSON_PROPERTY_NAME = "user"; //$NON-NLS-1$

    /** user json property */
    public static final StringProperty USER_JSON_PROPERTY = new StringProperty(null, USER_JSON_PROPERTY_NAME);

}
