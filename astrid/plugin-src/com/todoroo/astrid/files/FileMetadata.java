package com.todoroo.astrid.files;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Metadata;

public class FileMetadata {

    /** metadata key */
    public static final String METADATA_KEY = "file"; //$NON-NLS-1$

    /** Constants for file types */
    public static final int FILE_TYPE_AUDIO = 0;
    public static final int FILE_TYPE_IMG = 1;
    public static final int FILE_TYPE_OTHER = 2;

    public static final StringProperty FILE_PATH = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    public static final IntegerProperty FILE_TYPE = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    public static final LongProperty ATTACH_DATE = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    public static Metadata createNewFileMetadata(long taskId, String filePath, int fileType) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, METADATA_KEY);
        metadata.setValue(Metadata.TASK, taskId);
        metadata.setValue(FILE_PATH, filePath);
        metadata.setValue(FILE_TYPE, fileType);
        metadata.setValue(ATTACH_DATE, DateUtilities.now());
        return metadata;
    }

}
