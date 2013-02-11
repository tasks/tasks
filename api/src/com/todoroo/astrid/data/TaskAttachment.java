/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;


import android.content.ContentValues;
import android.net.Uri;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;

/**
 * Data Model which represents a user.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public final class TaskAttachment extends RemoteModel {

    // --- table and uri

    /** table for this model */
    public static final Table TABLE = new Table("task_attachments", TaskAttachment.class);

    /** model class for entries in the outstanding table */
    public static final Class<? extends OutstandingEntry<TaskAttachment>> OUTSTANDING_MODEL = TaskAttachmentOutstanding.class;

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.API_PACKAGE + "/" +
            TABLE.name);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Remote id */
    public static final StringProperty UUID = new StringProperty(
            TABLE, UUID_PROPERTY_NAME);

    /** Pushed at date */
    public static final LongProperty PUSHED_AT = new LongProperty(
            TABLE, PUSHED_AT_PROPERTY_NAME);

    /** Creator user id */
    public static final StringProperty USER_UUID = new StringProperty(
            TABLE, "user_id");

    /** Task uuid */
    public static final StringProperty TASK_UUID = new StringProperty(
            TABLE, "task_id");

    /** File name */
    public static final StringProperty NAME = new StringProperty(
            TABLE, "name");

    /** File url (for downloading) */
    public static final StringProperty URL = new StringProperty(
            TABLE, "url");

    /** File path (on local storage) */
    public static final StringProperty FILE_PATH = new StringProperty(
            TABLE, "path");

    /** File size (in bytes) */
    public static final IntegerProperty SIZE = new IntegerProperty(
            TABLE, "size");

    /** File mimetype */
    public static final StringProperty CONTENT_TYPE = new StringProperty(
            TABLE, "content_type");

    /** Attachment creation date */
    public static final LongProperty CREATED_AT = new LongProperty(
            TABLE, "created_at", Property.PROP_FLAG_DATE);

    /** Attachment deletion date */
    public static final LongProperty DELETED_AT = new LongProperty(
            TABLE, "deleted_at", Property.PROP_FLAG_DATE);

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(TaskAttachment.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(UUID.name, NO_UUID);
        defaultValues.put(PUSHED_AT.name, 0);
        defaultValues.put(USER_UUID.name, NO_UUID);
        defaultValues.put(TASK_UUID.name, NO_UUID);
        defaultValues.put(NAME.name, "");
        defaultValues.put(URL.name, "");
        defaultValues.put(FILE_PATH.name, "");
        defaultValues.put(SIZE.name, 0);
        defaultValues.put(CONTENT_TYPE.name, "");
        defaultValues.put(CREATED_AT.name, 0);
        defaultValues.put(DELETED_AT.name, 0);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // -- Constants
    /** default directory for files on external storage */
    public static final String FILES_DIRECTORY_DEFAULT = "attachments"; //$NON-NLS-1$

    /** preference key for some other download directory */
    public static final String FILES_DIRECTORY_PREF = "custom_files_dir"; //$NON-NLS-1$

    /** Constants for file types */
    public static final String FILE_TYPE_AUDIO = "audio/"; //$NON-NLS-1$
    public static final String FILE_TYPE_IMAGE = "image/"; //$NON-NLS-1$
    public static final String FILE_TYPE_PDF = "application/pdf"; //$NON-NLS-1$

    public static final String FILE_TYPE_DOC = "application/msword";  //$NON-NLS-1$
    public static final String FILE_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";  //$NON-NLS-1$
    public static final String FILE_TYPE_PPT = "application/vnd.ms-powerpoint";  //$NON-NLS-1$
    public static final String FILE_TYPE_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";  //$NON-NLS-1$
    public static final String FILE_TYPE_XLS = "application/vnd.ms-excel"; //$NON-NLS-1$
    public static final String FILE_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; //$NON-NLS-1$

    public static final String[] MS_FILETYPES = {
        FILE_TYPE_DOC, FILE_TYPE_DOCX,
        FILE_TYPE_XLS, FILE_TYPE_XLSX,
        FILE_TYPE_PPT, FILE_TYPE_PPTX,
    };

    public static final String FILE_TYPE_OTHER = "application/octet-stream";  //$NON-NLS-1$

    public static TaskAttachment createNewAttachment(String taskUuid, String filePath, String fileName, String fileType) {
        TaskAttachment attachment = new TaskAttachment();
        attachment.setValue(TaskAttachment.TASK_UUID, taskUuid);
        attachment.setValue(NAME, fileName);
        attachment.setValue(USER_UUID, Task.USER_ID_SELF);
        attachment.setValue(FILE_PATH, filePath);
        attachment.setValue(CONTENT_TYPE, fileType);
        attachment.setValue(CREATED_AT, DateUtilities.now());
        attachment.setValue(DELETED_AT, 0L);
        return attachment;
    }

    // --- data access boilerplate

    public TaskAttachment() {
        super();
    }

    public TaskAttachment(TodorooCursor<TaskAttachment> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<TaskAttachment> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    @Override
    public String getUuid() {
        return getUuidHelper(UUID);
    }

    // --- parcelable helpers

    public static final Creator<TaskAttachment> CREATOR = new ModelCreator<TaskAttachment>(TaskAttachment.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
