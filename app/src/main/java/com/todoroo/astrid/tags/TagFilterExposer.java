/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import com.google.common.base.Strings;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.TagFilter;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.TagData;

/**
 * Exposes filters based on tags
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class TagFilterExposer {

  private final TagService tagService;

  @Inject
  public TagFilterExposer(TagService tagService) {
    this.tagService = tagService;
  }

  /** Create filter from new tag object */
  private static TagFilter filterFromTag(TagData tag) {
    if (tag == null || Strings.isNullOrEmpty(tag.getName())) {
      return null;
    }
    return new TagFilter(tag);
  }

  public List<Filter> getFilters() {
    return filterFromTags(tagService.getTagList());
  }

  public Filter getFilterByUuid(String uuid) {
    return filterFromTag(tagService.tagFromUUID(uuid));
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
