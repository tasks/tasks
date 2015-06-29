/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;


import android.content.ContentValues;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;

/**
 * Data Model which represents a user.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TaskAttachment extends RemoteModel {

    // --- table and uri

    /** table for this model */
    public static final Table TABLE = new Table("task_attachments", TaskAttachment.class);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Remote id */
    public static final StringProperty UUID = new StringProperty(
            TABLE, UUID_PROPERTY_NAME);

    /** Task uuid */
    public static final StringProperty TASK_UUID = new StringProperty(
            TABLE, "task_id");

    /** File name */
    public static final StringProperty NAME = new StringProperty(
            TABLE, "name");

    /** File path (on local storage) */
    public static final StringProperty FILE_PATH = new StringProperty(
            TABLE, "path");

    /** File mimetype */
    public static final StringProperty CONTENT_TYPE = new StringProperty(
            TABLE, "content_type");

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
        defaultValues.put(TASK_UUID.name, NO_UUID);
        defaultValues.put(NAME.name, "");
        defaultValues.put(FILE_PATH.name, "");
        defaultValues.put(CONTENT_TYPE.name, "");
        defaultValues.put(DELETED_AT.name, 0);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // -- Constants
    /** default directory for files on external storage */
    public static final String FILES_DIRECTORY_DEFAULT = "attachments"; //$NON-NLS-1$

    /** Constants for file types */
    public static final String FILE_TYPE_AUDIO = "audio/"; //$NON-NLS-1$
    public static final String FILE_TYPE_IMAGE = "image/"; //$NON-NLS-1$

    public static final String FILE_TYPE_OTHER = "application/octet-stream";  //$NON-NLS-1$

    public static TaskAttachment createNewAttachment(String taskUuid, String filePath, String fileName, String fileType) {
        TaskAttachment attachment = new TaskAttachment();
        attachment.setTaskUUID(taskUuid);
        attachment.setName(fileName);
        attachment.setFilePath(filePath);
        attachment.setContentType(fileType);
        attachment.setDeletedAt(0L);
        return attachment;
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    // --- parcelable helpers

    public static final Creator<TaskAttachment> CREATOR = new ModelCreator<>(TaskAttachment.class);

    public void setDeletedAt(Long deletedAt) {
        setValue(DELETED_AT, deletedAt);
    }

    public void setTaskUUID(String taskUuid) {
        setValue(TASK_UUID, taskUuid);
    }

    public String getName() {
        return getValue(NAME);
    }

    public void setName(String name) {
        setValue(NAME, name);
    }

    public String getContentType() {
        return getValue(CONTENT_TYPE);
    }

    public void setContentType(String contentType) {
        setValue(CONTENT_TYPE, contentType);
    }

    public String getUUID() {
        return getValue(UUID);
    }

    public String getFilePath() {
        return getValue(FILE_PATH);
    }

    public void setFilePath(String filePath) {
        setValue(FILE_PATH, filePath);
    }
}
