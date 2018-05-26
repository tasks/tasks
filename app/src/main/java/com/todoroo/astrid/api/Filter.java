/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;
import com.todoroo.andlib.sql.QueryTemplate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A <code>FilterListFilter</code> allows users to display tasks that have something in common.
 *
 * <p>A plug-in can expose new <code>FilterListFilter</code>s to the system by responding to the
 * <code>com.todoroo.astrid.GET_FILTERS</code> broadcast intent.
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class Filter extends FilterListItem {

  /** Parcelable Creator Object */
  public static final Parcelable.Creator<Filter> CREATOR =
      new Parcelable.Creator<Filter>() {

        /** {@inheritDoc} */
        @Override
        public Filter createFromParcel(Parcel source) {
          Filter item = new Filter();
          item.readFromParcel(source);
          return item;
        }

        /** {@inheritDoc} */
        @Override
        public Filter[] newArray(int size) {
          return new Filter[size];
        }
      };
  /**
   * Values to apply to a task when quick-adding a task from this filter. For example, when a user
   * views tasks tagged 'ABC', the tasks they create should also be tagged 'ABC'. If set to null, no
   * additional values will be stored for a task. Can use {@link PermaSql}
   */
  public final Map<String, Object> valuesForNewTasks = new HashMap<>();
  /**
   * {@link PermaSql} query for this filter. The query will be appended to the select statement
   * after "<code>SELECT fields FROM table %s</code>". It is recommended that you use a {@link
   * QueryTemplate} to construct your query.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li><code>"WHERE completionDate = 0"</code>
   *   <li><code>"INNER JOIN " +
   * Constants.TABLE_METADATA + " ON metadata.task = tasks.id WHERE
   * metadata.namespace = " + NAMESPACE + " AND metadata.key = 'a' AND
   * metadata.value = 'b' GROUP BY tasks.id ORDER BY tasks.title"</code>
   * </ul>
   */
  String sqlQuery;
  /**
   * Field for holding a modified sqlQuery based on sqlQuery. Useful for adjusting query for
   * sort/subtasks without breaking the equality checking based on sqlQuery.
   */
  private String filterOverride;

  public Filter(String listingTitle, QueryTemplate sqlQuery) {
    this(listingTitle, sqlQuery, Collections.emptyMap());
  }

  /**
   * Utility constructor for creating a Filter object
   *
   * @param listingTitle Title of this item as displayed on the lists page, e.g. Inbox
   * @param sqlQuery SQL query for this list (see {@link #sqlQuery} for examples).
   */
  public Filter(
      String listingTitle, QueryTemplate sqlQuery, Map<String, Object> valuesForNewTasks) {
    this(listingTitle, sqlQuery == null ? null : sqlQuery.toString(), valuesForNewTasks);
  }

  /**
   * Utility constructor for creating a Filter object
   *
   * @param listingTitle Title of this item as displayed on the lists page, e.g. Inbox
   * @param sqlQuery SQL query for this list (see {@link #sqlQuery} for examples).
   */
  Filter(String listingTitle, String sqlQuery, Map<String, Object> valuesForNewTasks) {
    this.listingTitle = listingTitle;
    this.sqlQuery = sqlQuery;
    this.filterOverride = null;
    if (valuesForNewTasks != null) {
      this.valuesForNewTasks.putAll(valuesForNewTasks);
    }
  }

  /** Utility constructor */
  Filter() {
    // do nothing
  }

  public String getSqlQuery() {
    if (filterOverride != null) {
      return filterOverride;
    }
    return sqlQuery;
  }

  // --- parcelable

  public void setFilterQueryOverride(String filterOverride) {
    this.filterOverride = filterOverride;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((sqlQuery == null) ? 0 : sqlQuery.hashCode());
    result = prime * result + ((listingTitle == null) ? 0 : listingTitle.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Filter other = (Filter) obj;
    if (sqlQuery == null) {
      if (other.sqlQuery != null) {
        return false;
      }
    } else if (!sqlQuery.equals(other.sqlQuery)) {
      return false;
    }
    if (listingTitle == null) {
      if (other.listingTitle != null) {
        return false;
      }
    } else if (!listingTitle.equals(other.listingTitle)) {
      return false;
    }
    return true;
  }

  @Override
  public Type getItemType() {
    return Type.ITEM;
  }

  /** {@inheritDoc} */
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeString(""); // old title
    dest.writeString(sqlQuery);
    dest.writeMap(valuesForNewTasks);
  }

  @Override
  protected void readFromParcel(Parcel source) {
    super.readFromParcel(source);
    source.readString(); // old title
    sqlQuery = source.readString();
    source.readMap(valuesForNewTasks, getClass().getClassLoader());
  }

  public boolean supportsSubtasks() {
    return false;
  }

  @Override
  public String toString() {
    return "Filter{"
        + "sqlQuery='"
        + sqlQuery
        + '\''
        + ", filterOverride='"
        + filterOverride
        + '\''
        + ", valuesForNewTasks="
        + valuesForNewTasks
        + '}';
  }
}
