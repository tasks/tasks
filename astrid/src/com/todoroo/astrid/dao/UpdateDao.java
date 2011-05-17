/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.DatabaseDao;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.data.Update;

/**
 * Data Access layer for {@link Update}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class UpdateDao extends DatabaseDao<Update> {

    @Autowired Database database;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="UR_UNINIT_READ")
	public UpdateDao() {
        super(Update.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

}

