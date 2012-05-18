package com.todoroo.astrid.files;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.data.Metadata;

public class FileMetadata {

    /** metadata key */
    public static final String METADATA_KEY = "file"; //$NON-NLS-1$

    /** Constants for file types */
    public static final int FILE_TYPE_AUDIO = 0;
    public static final int FILE_TYPE_PDF = 1;

    public static final StringProperty FILE_PATH = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    public static final LongProperty FILE_TYPE = new LongProperty(Metadata.TABLE,
            Metadata.VALUE2.name);


    public static Metadata createNewFileMetadata(long taskId, String filePath, long fileType) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, METADATA_KEY);
        metadata.setValue(Metadata.TASK, taskId);
        metadata.setValue(FILE_PATH, filePath);
        metadata.setValue(FILE_TYPE, fileType);
        return metadata;
    }

}
