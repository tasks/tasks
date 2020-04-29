package org.tasks.filters;

import androidx.room.Embedded;
import com.todoroo.astrid.api.TagFilter;
import java.util.Objects;
import org.tasks.data.TagData;

public class TagFilters {
  @Embedded public TagData tagData;
  public int count;

  TagFilter toTagFilter() {
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
    return count == that.count && Objects.equals(tagData, that.tagData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tagData, count);
  }

  @Override
  public String toString() {
    return "TagFilters{" + "tagData=" + tagData + ", count=" + count + '}';
  }
}
