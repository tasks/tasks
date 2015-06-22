/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import com.todoroo.andlib.data.Callback;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.StoreObjectDao;
import com.todoroo.astrid.data.StoreObject;

import org.tasks.R;
import org.tasks.preferences.ResourceResolver;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public final class CustomFilterExposer {

    private final StoreObjectDao storeObjectDao;
    private ResourceResolver resourceResolver;

    @Inject
    public CustomFilterExposer(ResourceResolver resourceResolver, StoreObjectDao storeObjectDao) {
        this.resourceResolver = resourceResolver;
        this.storeObjectDao = storeObjectDao;
    }

    public List<Filter> getFilters() {
        final List<Filter> list = new ArrayList<>();

        final int filter = resourceResolver.getResource(R.attr.ic_action_filter);

        storeObjectDao.getSavedFilters(new Callback<StoreObject>() {
            @Override
            public void apply(StoreObject savedFilter) {
                CustomFilter f = SavedFilter.load(savedFilter);
                f.icon = filter;
                list.add(f);
            }
        });

        return list;
    }
}
