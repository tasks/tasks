/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.data.TaskAttachment;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaskAttachmentDao extends RemoteModelDao<TaskAttachment> {

    @Inject
    public TaskAttachmentDao(Database database) {
        super(TaskAttachment.class);
        setDatabase(database);
    }

    public boolean taskHasAttachments(String taskUuid) {
        return count(byUuid(taskUuid, TaskAttachment.TASK_UUID).limit(1)) > 0;
    }

    public void getAttachments(String taskUuid, Callback<TaskAttachment> callback) {
        query(callback, byUuid(taskUuid, TaskAttachment.PROPERTIES));
    }

    private static Query byUuid(String taskUuid, Property<?>... properties) {
        return Query.select(properties).where(
                Criterion.and(TaskAttachment.TASK_UUID.eq(taskUuid),
                        TaskAttachment.DELETED_AT.eq(0))
        );
    }
}

