/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.GtasksFilter;

import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;

/**
 * Exposes filters based on lists
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksFilterExposer {

    private final GtasksListService gtasksListService;
    private final GtasksPreferenceService gtasksPreferenceService;

    @Inject
    public GtasksFilterExposer(GtasksListService gtasksListService, GtasksPreferenceService gtasksPreferenceService) {
        this.gtasksListService = gtasksListService;
        this.gtasksPreferenceService = gtasksPreferenceService;
    }

    public List<Filter> getFilters() {
        // if we aren't logged in (or we are logged in to astrid.com), don't expose features
        if(!gtasksPreferenceService.isLoggedIn()) {
            return emptyList();
        }

        List<Filter> listFilters = newArrayList();
        for (GtasksList list : gtasksListService.getLists()) {
            listFilters.add(filterFromList(list));
        }
        return listFilters;
    }

    public Filter getFilter(long id) {
        if (gtasksPreferenceService.isLoggedIn()) {
            GtasksList list = gtasksListService.getList(id);
            if (list != null) {
                return filterFromList(list);
            }
        }
        return null;
    }

    private Filter filterFromList(GtasksList list) {
        return new GtasksFilter(list);
    }
}
