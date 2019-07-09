/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.core;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

import com.todoroo.astrid.api.CustomFilter;
import com.todoroo.astrid.api.Filter;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.FilterDao;

public final class CustomFilterExposer {

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
    return savedFilter == null ? null : new CustomFilter(savedFilter);
  }
}
