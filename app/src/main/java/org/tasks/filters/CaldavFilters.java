package org.tasks.filters;

import androidx.room.Embedded;
import com.todoroo.astrid.api.CaldavFilter;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;

public class CaldavFilters {
  @Embedded public CaldavCalendar caldavCalendar;
  @Embedded public CaldavAccount caldavAccount;
  public int count;

  public CaldavFilter toCaldavFilter() {
    CaldavFilter filter = new CaldavFilter(caldavCalendar);
    filter.count = count;
    return filter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CaldavFilters that = (CaldavFilters) o;

    if (count != that.count) {
      return false;
    }
    if (caldavCalendar != null
        ? !caldavCalendar.equals(that.caldavCalendar)
        : that.caldavCalendar != null) {
      return false;
    }
    return caldavAccount != null
        ? caldavAccount.equals(that.caldavAccount)
        : that.caldavAccount == null;
  }

  @Override
  public int hashCode() {
    int result = caldavCalendar != null ? caldavCalendar.hashCode() : 0;
    result = 31 * result + (caldavAccount != null ? caldavAccount.hashCode() : 0);
    result = 31 * result + count;
    return result;
  }

  @Override
  public String toString() {
    return "CaldavFilters{"
        + "caldavCalendar="
        + caldavCalendar
        + ", caldavAccount="
        + caldavAccount
        + ", count="
        + count
        + '}';
  }
}
