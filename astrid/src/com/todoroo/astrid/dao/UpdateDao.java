/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.dao;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.data.Update;


/**
 * Data Access layer for {@link Update}-related operations.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@Deprecated
public class UpdateDao extends RemoteModelDao<Update> {

    @Autowired Database database;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="UR_UNINIT_READ")
	public UpdateDao() {
        super(Update.class);
        DependencyInjectionService.getInstance().inject(this);
        setDatabase(database);
    }

}

