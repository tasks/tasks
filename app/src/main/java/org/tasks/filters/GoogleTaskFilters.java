package org.tasks.filters;

import androidx.room.Embedded;
import com.todoroo.astrid.api.GtasksFilter;
import org.tasks.data.GoogleTaskList;

public class GoogleTaskFilters {
  @Embedded public GoogleTaskList googleTaskList;
  public int count;

  public GtasksFilter toGtasksFilter() {
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

    if (count != that.count) {
      return false;
    }
    return googleTaskList != null
        ? googleTaskList.equals(that.googleTaskList)
        : that.googleTaskList == null;
  }

  @Override
  public int hashCode() {
    int result = googleTaskList != null ? googleTaskList.hashCode() : 0;
    result = 31 * result + count;
    return result;
  }

  @Override
  public String toString() {
    return "GoogleTaskFilters{" + "googleTaskList=" + googleTaskList + ", count=" + count + '}';
  }
}
