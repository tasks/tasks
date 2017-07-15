/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import com.google.common.base.Strings;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.data.TagData;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Exposes filters based on tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagFilterExposer {

    private final TagService tagService;

    @Inject
    public TagFilterExposer(TagService tagService) {
        this.tagService = tagService;
    }

    public List<Filter> getFilters() {
        ArrayList<Filter> list = new ArrayList<>();

        list.addAll(filterFromTags(tagService.getTagList()));

        // transmit filter list
        return list;
    }

    public Filter getFilterByUuid(String uuid) {
        return filterFromTag(tagService.tagFromUUID(uuid));
    }

    /** Create filter from new tag object */
    private static TagFilter filterFromTag(TagData tag) {
        if (tag == null || Strings.isNullOrEmpty(tag.getName())) {
            return null;
        }
        return new TagFilter(tag);
    }

    private List<Filter> filterFromTags(List<TagData> tags) {
        List<Filter> filters = new ArrayList<>();
        for (TagData tag : tags) {
            Filter f = filterFromTag(tag);
            if (f != null) {
                filters.add(f);
            }
        }
        return filters;
    }
}
