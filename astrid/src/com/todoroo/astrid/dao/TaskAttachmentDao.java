/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.text.TextUtils;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.data.OutstandingEntry;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TaskAttachment;
import com.todoroo.astrid.data.TaskAttachmentOutstanding;

/**
 * Data Access layer for {@link TagData}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskAttachmentDao extends RemoteModelDao<TaskAttachment> {

    @Autowired Database database;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="UR_UNINIT_READ")
	public TaskAttachmentDao() {
        super(TaskAttachment.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    @Override
    protected boolean shouldRecordOutstandingEntry(String columnName, Object value) {
        return NameMaps.shouldRecordOutstandingColumnForTable(NameMaps.TABLE_ID_ATTACHMENTS, columnName);
    }

    @Override
    protected int createOutstandingEntries(long modelId, ContentValues modelSetValues) {
        // new attachment case -- only set by us when creating new attachments; when setting during sync outstanding entries suppressed
        if (modelSetValues.containsKey(TaskAttachment.CONTENT_TYPE.name)) {
            TaskAttachmentOutstanding m = new TaskAttachmentOutstanding();
            String path = modelSetValues.getAsString(TaskAttachment.FILE_PATH.name);
            if (TextUtils.isEmpty(path))
                return -1;
            try {
                JSONObject newFileHash = new JSONObject();
                newFileHash.put("name", modelSetValues.getAsString(TaskAttachment.NAME.name)); //$NON-NLS-1$
                newFileHash.put("type", modelSetValues.getAsString(TaskAttachment.CONTENT_TYPE.name)); //$NON-NLS-1$
                newFileHash.put("path", path); //$NON-NLS-1$

                m.setValue(OutstandingEntry.ENTITY_ID_PROPERTY, modelId);
                m.setValue(OutstandingEntry.COLUMN_STRING_PROPERTY, NameMaps.ATTACHMENT_ADDED_COLUMN);
                m.setValue(OutstandingEntry.VALUE_STRING_PROPERTY, newFileHash.toString());
                m.setValue(OutstandingEntry.CREATED_AT_PROPERTY, DateUtilities.now());
                database.insert(outstandingTable.name, null, m.getSetValues());
            } catch (JSONException e) {
                return -1;
            }
        }
        int result = super.createOutstandingEntries(modelId, modelSetValues);
        if (result < 0) // Error
            return result;
        return 1 + result;
    }

    public boolean taskHasAttachments(String taskUuid) {
        TodorooCursor<TaskAttachment> files = query(Query.select(TaskAttachment.TASK_UUID).where(
                        Criterion.and(TaskAttachment.TASK_UUID.eq(taskUuid),
                                TaskAttachment.DELETED_AT.eq(0))).limit(1));
        try {
            return files.getCount() > 0;
        } finally {
            files.close();
        }
    }

}

