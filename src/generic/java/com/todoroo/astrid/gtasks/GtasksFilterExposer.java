package com.todoroo.astrid.gtasks;

import com.todoroo.astrid.api.Filter;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class GtasksFilterExposer {
    @Inject
    public GtasksFilterExposer() {

    }

    public List<Filter> getFilters() {
        return Collections.emptyList();
    }

    public Filter getFilter(long aLong) {
        return null;
    }
}
