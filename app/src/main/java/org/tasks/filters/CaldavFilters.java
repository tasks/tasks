package org.tasks.filters;

import androidx.room.Embedded;
import com.todoroo.astrid.api.CaldavFilter;
import org.tasks.data.CaldavCalendar;

public class CaldavFilters {
  @Embedded public CaldavCalendar caldavCalendar;
  public int count;

  CaldavFilter toCaldavFilter() {
    CaldavFilter filter = new CaldavFilter(caldavCalendar);
    filter.count = count;
    return filter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CaldavFilters)) {
      return false;
    }

    CaldavFilters that = (CaldavFilters) o;

    if (count != that.count) {
      return false;
    }
    return caldavCalendar != null
        ? caldavCalendar.equals(that.caldavCalendar)
        : that.caldavCalendar == null;
  }

  @Override
  public int hashCode() {
    int result = caldavCalendar != null ? caldavCalendar.hashCode() : 0;
    result = 31 * result + count;
    return result;
  }

  @Override
  public String toString() {
    return "CaldavFilters{" + "caldavCalendar=" + caldavCalendar + ", count=" + count + '}';
  }
}
