package org.tasks.filters;

import androidx.room.Embedded;
import org.tasks.data.GoogleTaskAccount;
import org.tasks.data.GoogleTaskList;

public class GoogleTaskFilters {
  @Embedded public GoogleTaskList googleTaskList;
  @Embedded public GoogleTaskAccount googleTaskAccount;
  public int count;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    GoogleTaskFilters that = (GoogleTaskFilters) o;

    if (count != that.count) {
      return false;
    }
    if (googleTaskList != null
        ? !googleTaskList.equals(that.googleTaskList)
        : that.googleTaskList != null) {
      return false;
    }
    return googleTaskAccount != null
        ? googleTaskAccount.equals(that.googleTaskAccount)
        : that.googleTaskAccount == null;
  }

  @Override
  public int hashCode() {
    int result = googleTaskList != null ? googleTaskList.hashCode() : 0;
    result = 31 * result + (googleTaskAccount != null ? googleTaskAccount.hashCode() : 0);
    result = 31 * result + count;
    return result;
  }

  @Override
  public String toString() {
    return "GoogleTaskFilters{"
        + "googleTaskList="
        + googleTaskList
        + ", googleTaskAccount="
        + googleTaskAccount
        + ", count="
        + count
        + '}';
  }
}
