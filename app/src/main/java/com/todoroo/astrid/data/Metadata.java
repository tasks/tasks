/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.content.ContentValues;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;

/**
 * Data Model which represents a piece of metadata associated with a task
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Entity(tableName = "metadata",
        indices = {
                @Index(name = "md_tid", value = "task"),
                @Index(name = "md_tkid", value = {"task", "key"})
        })
public class Metadata extends AbstractModel {

    // --- table

    /** table for this model */
    public static final Table TABLE = new Table("metadata", Metadata.class);

    // --- properties

    /** ID */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public Long id;
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Associated Task */
    @ColumnInfo(name = "task")
    public Long task;
    public static final LongProperty TASK = new LongProperty(
            TABLE, "task");

    /** Metadata Key */
    @ColumnInfo(name = "key")
    public String key;
    public static final StringProperty KEY = new StringProperty(
            TABLE, "key");

    /** Metadata Text Value Column 1 */
    @ColumnInfo(name = "value")
    public String value1;
    public static final StringProperty VALUE1 = new StringProperty(
            TABLE, "value");

    /** Metadata Text Value Column 2 */
    @ColumnInfo(name = "value2")
    public String value2;
    public static final StringProperty VALUE2 = new StringProperty(
            TABLE, "value2");

    /** Metadata Text Value Column 3 */
    @ColumnInfo(name = "value3")
    public String value3;
    public static final StringProperty VALUE3 = new StringProperty(
            TABLE, "value3");

    /** Metadata Text Value Column 4 */
    @ColumnInfo(name = "value4")
    public String value4;
    public static final StringProperty VALUE4 = new StringProperty(
            TABLE, "value4");

    /** Metadata Text Value Column 5 */
    @ColumnInfo(name = "value5")
    public String value5;
    public static final StringProperty VALUE5 = new StringProperty(
            TABLE, "value5");

    @ColumnInfo(name = "value6")
    public String value6;
    public static final StringProperty VALUE6 = new StringProperty(
            TABLE, "value6");

    @ColumnInfo(name = "value7")
    public String value7;
    public static final StringProperty VALUE7 = new StringProperty(
            TABLE, "value7");

    /** Unixtime Metadata was created */
    @ColumnInfo(name = "created")
    public Long created;
    public static final LongProperty CREATION_DATE = new LongProperty(
            TABLE, "created");

    /** Unixtime metadata was deleted/tombstoned */
    @ColumnInfo(name = "deleted")
    public Long deleted = 0L;
    public static final LongProperty DELETION_DATE = new LongProperty(
            TABLE, "deleted");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(Metadata.class);

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(DELETION_DATE.name, 0L);
    }

    public Metadata() {
        super();
    }

    @Ignore
    public Metadata(Metadata metadata) {
        super(metadata);
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    // --- parcelable helpers

    private static final Creator<Metadata> CREATOR = new ModelCreator<>(Metadata.class);

    public Long getDeletionDate() {
        return getValue(DELETION_DATE);
    }

    public void setDeletionDate(Long deletionDate) {
        setValue(DELETION_DATE, deletionDate);
    }

    public Long getTask() {
        return getValue(TASK);
    }

    public void setTask(Long task) {
        setValue(TASK, task);
    }

    public Long getCreationDate() {
        return getValue(CREATION_DATE);
    }

    public void setCreationDate(Long creationDate) {
        setValue(CREATION_DATE, creationDate);
    }

    public String getKey() {
        return getValue(KEY);
    }

    public void setKey(String key) {
        setValue(KEY, key);
    }
}
