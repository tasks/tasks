package com.todoroo.astrid.tags;

import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.TagMetadata;

public class TagMemberMetadata {

    public static final String KEY = "tag-members"; //$NON-NLS-1$

    public static final StringProperty USER_UUID = new StringProperty(
            TagMetadata.TABLE, TagMetadata.VALUE1.name);
}
