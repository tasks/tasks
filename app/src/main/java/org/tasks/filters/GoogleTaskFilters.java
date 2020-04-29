package org.tasks.filters;

import androidx.room.Embedded;
import com.todoroo.astrid.api.GtasksFilter;
import java.util.Objects;
import org.tasks.data.GoogleTaskList;

public class GoogleTaskFilters {
  @Embedded public GoogleTaskList googleTaskList;
  public int count;

  GtasksFilter toGtasksFilter() {
    GtasksFilter filter = new GtasksFilter(googleTaskList);
    filter.count = count;
    return filter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GoogleTaskFilters)) {
      return false;
    }
    GoogleTaskFilters that = (GoogleTaskFilters) o;
    return count == that.count && Objects.equals(googleTaskList, that.googleTaskList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(googleTaskList, count);
  }

  @Override
  public String toString() {
    return "GoogleTaskFilters{" + "googleTaskList=" + googleTaskList + ", count=" + count + '}';
  }
}
