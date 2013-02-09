/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;

/**
 * This class was deprecated with SyncV2. Use TaskAttachment instead.
 * @author Sam
 *
 */
@Deprecated
public class FileMetadata {

    /** metadata key */
    public static final String METADATA_KEY = "file"; //$NON-NLS-1$

    public static final StringProperty FILE_PATH = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    public static final StringProperty FILE_TYPE = new StringProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    public static final LongProperty DELETION_DATE = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    public static final LongProperty REMOTE_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

    public static final StringProperty URL = new StringProperty(Metadata.TABLE,
            Metadata.VALUE5.name);

    public static final StringProperty NAME = new StringProperty(Metadata.TABLE,
            Metadata.VALUE6.name);

}
