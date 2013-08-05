/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data;

import android.content.Context;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;

/**
 * Abstract controller class. Mostly contains some static fields
 */

abstract public class LegacyAbstractController {

    protected Context context;

    // special columns
    public static final String KEY_ROWID = "_id";

    // database and table names

    @Autowired
    protected String tasksTable;

    @Autowired
    protected String tagsTable;

    @Autowired
    protected String tagTaskTable;

    @Autowired
    protected String alertsTable;

    @Autowired
    protected String syncTable;

    // stuff

    public LegacyAbstractController(Context context) {
        this.context = context;
        DependencyInjectionService.getInstance().inject(this);
    }

    abstract public void open();

    abstract public void close();
}
