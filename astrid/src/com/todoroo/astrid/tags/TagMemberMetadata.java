package com.todoroo.astrid.tags;

import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.TagMetadata;

public class TagMemberMetadata {

    public static final String KEY = "tag-members"; //$NON-NLS-1$

    public static final StringProperty USER_UUID = new StringProperty(
            TagMetadata.TABLE, TagMetadata.VALUE1.name);

    public static TagMetadata newMemberMetadata(long tagId, String tagUuid, String userUuid) {
        TagMetadata m = new TagMetadata();
        m.setValue(TagMetadata.KEY, KEY);
        m.setValue(TagMetadata.TAG_ID, tagId);
        m.setValue(TagMetadata.TAG_UUID, tagUuid);
        m.setValue(USER_UUID, userUuid);
        m.setValue(TagMetadata.DELETION_DATE, 0L);
        return m;
    }
}
