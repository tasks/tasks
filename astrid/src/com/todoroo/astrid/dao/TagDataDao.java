/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.TagData;

/**
 * Data Access layer for {@link TagData}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagDataDao extends DatabaseDao<TagData> {

    @Autowired Database database;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="UR_UNINIT_READ")
	public TagDataDao() {
        super(TagData.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

    private static final String[] IGNORE_OUTSTANDING_COLUMNS = new String[] {
        TagData.MODIFICATION_DATE.name,
        TagData.UUID.name
    };

    @Override
    protected boolean recordOutstandingEntry(String columnName) {
        return AndroidUtilities.indexOf(IGNORE_OUTSTANDING_COLUMNS, columnName) < 0;
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

