package com.todoroo.astrid.producteev.sync;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.model.StoreObject;

/**
 * {@link StoreObject} entries for a Producteev Dashboard
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ProducteevDashboard {

    /** type*/
    public static final String TYPE = "pdv-dash"; //$NON-NLS-1$

    /** dashboard id in producteev */
    public static final LongProperty REMOTE_ID = new LongProperty(StoreObject.TABLE,
            StoreObject.ITEM.name);

    /** dashboard name */
    public static final StringProperty NAME = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE1.name);

    /** users (list in the format "id_user,name;id_user,name;") */
    public static final StringProperty USERS = new StringProperty(StoreObject.TABLE,
            StoreObject.VALUE2.name);

}
