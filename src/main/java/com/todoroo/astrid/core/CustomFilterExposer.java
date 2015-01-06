/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.Context;
import android.content.Intent;

import com.todoroo.andlib.data.Callback;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.data.StoreObject;

import org.tasks.R;
import org.tasks.injection.ForApplication;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Exposes Astrid's built in filters to the NavigationDrawerFragment
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class CustomFilterExposer {

    static final String TOKEN_FILTER_ID = "id"; //$NON-NLS-1$

    private final StoreObjectDao storeObjectDao;
    private final Context context;

    @Inject
    public CustomFilterExposer(@ForApplication Context context, StoreObjectDao storeObjectDao) {
        this.context = context;
        this.storeObjectDao = storeObjectDao;
    }

    public List<Filter> getFilters() {
        final List<Filter> list = new ArrayList<>();

        storeObjectDao.getSavedFilters(new Callback<StoreObject>() {
            @Override
            public void apply(StoreObject savedFilter) {
                Filter f = SavedFilter.load(savedFilter);

                Intent deleteIntent = new Intent(context, DeleteFilterActivity.class);
                deleteIntent.putExtra(TOKEN_FILTER_ID, savedFilter.getId());
                f.contextMenuLabels = new String[] { context.getString(R.string.BFE_Saved_delete) };
                f.contextMenuIntents = new Intent[] { deleteIntent };
                list.add(f);

            }
        });

        return list;
    }
}
