/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.TaskAttachment;

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
    protected boolean shouldRecordOutstandingEntry(String columnName) {
        return NameMaps.shouldRecordOutstandingColumnForTable(NameMaps.TABLE_ID_ATTACHMENTS, columnName);
    }

    // --- SQL clause generators

    /**
     * Generates SQL clauses
     */
    public static class TagDataCriteria {

    	/** @returns tasks by id */
    	public static Criterion byId(long id) {
    	    return TagData.ID.eq(id);
    	}

        public static Criterion isTeam() {
            return TagData.IS_TEAM.eq(1);
        }

    }

}

