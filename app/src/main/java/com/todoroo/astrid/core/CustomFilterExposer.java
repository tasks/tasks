/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.text.TextUtils;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.Filter;
import org.tasks.data.StoreObjectDao;
import org.tasks.data.StoreObject;

import org.tasks.R;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

public final class CustomFilterExposer {

    private static final int filter = R.drawable.ic_filter_list_24dp;

    private final StoreObjectDao storeObjectDao;

    @Inject
    public CustomFilterExposer(StoreObjectDao storeObjectDao) {
        this.storeObjectDao = storeObjectDao;
    }

    public List<Filter> getFilters() {
        return newArrayList(transform(storeObjectDao.getSavedFilters(), this::load));
    }

    public Filter getFilter(long id) {
        return load(storeObjectDao.getSavedFilterById(id));
    }

    private Filter load(StoreObject savedFilter) {
        if (savedFilter == null) {
            return null;
        }

        String title = savedFilter.getItem();
        String sql = savedFilter.getValue();
        String valuesString = savedFilter.getValue2();

        Map<String, Object> values = null;
        if(!TextUtils.isEmpty(valuesString)) {
            values = AndroidUtilities.mapFromSerializedString(valuesString);
        }

        sql = sql.replace("tasks.userId=0", "1"); // TODO: replace dirty hack for missing column

        CustomFilter customFilter = new CustomFilter(title, sql, values, savedFilter.getId());
        customFilter.icon = filter;
        return customFilter;
    }
}
