/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.data;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
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
@Entity(tableName = "task_list_metadata")
public class TaskListMetadata {

    public static final String FILTER_ID_ALL = "all";
    public static final String FILTER_ID_TODAY = "today";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @ColumnInfo(name = "remoteId")
    private String remoteId = RemoteModel.NO_UUID;

    @ColumnInfo(name = "tag_uuid")
    private String tagUuid = RemoteModel.NO_UUID;

    @ColumnInfo(name = "filter")
    private String filter = "";

    @ColumnInfo(name = "task_ids")
    private String taskIds = "[]";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }

    public String getTagUuid() {
        return tagUuid;
    }

    public void setTagUuid(String tagUuid) {
        this.tagUuid = tagUuid;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getTaskIds() {
        return taskIds;
    }

    public void setTaskIds(String taskIds) {
        this.taskIds = taskIds;
    }
}
