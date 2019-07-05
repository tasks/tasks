package org.tasks.filters;

import androidx.room.Embedded;
import com.todoroo.astrid.api.TagFilter;
import org.tasks.data.TagData;

public class TagFilters {
  @Embedded public TagData tagData;
  public int count;

  public TagFilter toTagFilter() {
    TagFilter filter = new TagFilter(tagData);
    filter.count = count;
    return filter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TagFilters)) {
      return false;
    }

    TagFilters that = (TagFilters) o;

    if (count != that.count) {
      return false;
    }
    return tagData != null ? tagData.equals(that.tagData) : that.tagData == null;
  }

  @Override
  public int hashCode() {
    int result = tagData != null ? tagData.hashCode() : 0;
    result = 31 * result + count;
    return result;
  }

  @Override
  public String toString() {
    return "TagFilters{" + "tagData=" + tagData + ", count=" + count + '}';
  }
}
