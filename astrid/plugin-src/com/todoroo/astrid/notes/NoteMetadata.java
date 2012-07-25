/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.notes;

import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entry for a note displayed by the Notes plugin.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class NoteMetadata {

    /** metadata key */
    public static final String METADATA_KEY = "note"; //$NON-NLS-1$

    /** note body */
    public static final StringProperty BODY = Metadata.VALUE1;

    /** note description (title, date, from, etc) */
    public static final StringProperty TITLE = Metadata.VALUE2;

    /** note thumbnail URL */
    public static final StringProperty THUMBNAIL = Metadata.VALUE3;

    /** note external id (use for your own purposes) */
    public static final StringProperty COMMENT_PICTURE = Metadata.VALUE6;

    /** note external provider (use for your own purposes) */
    public static final StringProperty EXT_PROVIDER = Metadata.VALUE4;

    /** note external id (use for your own purposes) */
    public static final StringProperty EXT_ID = Metadata.VALUE5;


}
