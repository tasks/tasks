package com.todoroo.astrid.data;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;

@SuppressWarnings("nls")
public abstract class OutstandingEntry<TYPE extends RemoteModel> extends AbstractModel {

    public static final String ENTITY_ID_PROPERTY_NAME = "entityId";

    public static final LongProperty ENTITY_ID_PROPERTY = new LongProperty(null, ENTITY_ID_PROPERTY_NAME);

    public static final String COLUMN_STRING_PROPERTY_NAME = "columnString";

    public static final StringProperty COLUMN_STRING_PROPERTY = new StringProperty(null, COLUMN_STRING_PROPERTY_NAME);

    public static final String VALUE_STRING_PROPERTY_NAME = "valueString";

    public static final StringProperty VALUE_STRING_PROPERTY = new StringProperty(null, VALUE_STRING_PROPERTY_NAME);

    public static final String CREATED_AT_PROPERTY_NAME = "createdAt";

    public static final LongProperty CREATED_AT_PROPERTY = new LongProperty(null, CREATED_AT_PROPERTY_NAME);

}
