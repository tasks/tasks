package com.todoroo.astrid.data;

import android.content.Context;

import com.todoroo.andlib.data.ContentResolverDao;

/**
 * Data access object for accessing Astrid's {@link Update} table.
 *
 * @author Andrey Marchenko <igendou@gmail.com>
 *
 */
public class UpdateApiDao extends ContentResolverDao<Update>{

    public UpdateApiDao(Context context) {
        super(Update.class, context, Update.CONTENT_URI);
    }

}
