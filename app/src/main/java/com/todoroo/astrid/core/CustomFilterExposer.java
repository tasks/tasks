/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.core;

import static com.google.common.collect.Lists.transform;

import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.Filter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.FilterDao;
import org.tasks.filters.AlphanumComparator;

public final class CustomFilterExposer {

  private final FilterDao filterDao;

  @Inject
  public CustomFilterExposer(FilterDao filterDao) {
    this.filterDao = filterDao;
  }

  public List<Filter> getFilters() {
    List<Filter> filters = new ArrayList<>(transform(filterDao.getFilters(), this::load));
    Collections.sort(filters, new AlphanumComparator<>(AlphanumComparator.FILTER));
    return filters;
  }

  public Filter getFilter(long id) {
    return load(filterDao.getById(id));
  }

  private Filter load(org.tasks.data.Filter savedFilter) {
    return savedFilter == null ? null : new CustomFilter(savedFilter);
  }
}
