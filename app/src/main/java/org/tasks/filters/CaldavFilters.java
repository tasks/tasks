package org.tasks.filters;

import androidx.room.Embedded;
import com.todoroo.astrid.api.CaldavFilter;
import java.util.Objects;
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
    return count == that.count && Objects.equals(caldavCalendar, that.caldavCalendar);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caldavCalendar, count);
  }

  @Override
  public String toString() {
    return "CaldavFilters{" + "caldavCalendar=" + caldavCalendar + ", count=" + count + '}';
  }
}
