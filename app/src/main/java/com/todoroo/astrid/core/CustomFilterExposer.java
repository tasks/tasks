/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

import android.text.TextUtils;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.Filter;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.FilterDao;

public final class CustomFilterExposer {

  private static final int filter = R.drawable.ic_filter_list_24dp;
  private final FilterDao filterDao;

  @Inject
  public CustomFilterExposer(FilterDao filterDao) {
    this.filterDao = filterDao;
  }

  public List<Filter> getFilters() {
    return newArrayList(transform(filterDao.getFilters(), this::load));
  }

  public Filter getFilter(long id) {
    return load(filterDao.getById(id));
  }

  private Filter load(org.tasks.data.Filter savedFilter) {
    if (savedFilter == null) {
      return null;
    }

    String title = savedFilter.getTitle();
    String sql = savedFilter.getSql();
    String valuesString = savedFilter.getValues();

    Map<String, Object> values = null;
    if (!TextUtils.isEmpty(valuesString)) {
      values = AndroidUtilities.mapFromSerializedString(valuesString);
    }

    sql = sql.replace("tasks.userId=0", "1"); // TODO: replace dirty hack for missing column

    CustomFilter customFilter =
        new CustomFilter(title, sql, values, savedFilter.getId(), savedFilter.getCriterion());
    customFilter.icon = filter;
    return customFilter;
  }
}
