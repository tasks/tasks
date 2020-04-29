/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class CustomFilterCriterion implements Parcelable {

  /**
   * Values to apply to a task when quick-adding a task from a filter created from this criterion. ?
   * will be replaced with the entry value. For example, when a user views tasks tagged 'ABC', the
   * tasks they create should also be tagged 'ABC'. If set to null, no additional values will be
   * stored for a task.
   */
  public final Map<String, Object> valuesForNewTasks = new HashMap<>();
  /**
   * Criteria Identifier. This identifier allows saved filters to be reloaded.
   *
   * <p>e.g "duedate"
   */
  public String identifier;
  /**
   * Criteria Title. If the title contains ?, this is replaced by the entry label string selected.
   *
   * <p>e.g "Due: ?"
   */
  public String text;
  /**
   * Criterion SQL. This query should return task id's. If this contains ?, it will be replaced by
   * the entry value
   *
   * <p>Examples:
   *
   * <ul>
   *   <li><code>SELECT _id FROM tasks WHERE dueDate <= ?</code>
   *   <li><code>SELECT task FROM metadata WHERE value = '?'</code>
   * </ul>
   */
  public String sql;
  /** Criteria name. This is displayed when users are selecting a criteria */
  public String name;

  // --- parcelable utilities

  public String getName() {
    return name;
  }

  /** Utility method to write to parcel */
  void writeToParcel(Parcel dest) {
    dest.writeString(identifier);
    dest.writeString(text);
    dest.writeString(sql);
    dest.writeMap(valuesForNewTasks);
    dest.writeString(name);
  }

  /** Utility method to read from parcel */
  void readFromParcel(Parcel source) {
    identifier = source.readString();
    text = source.readString();
    sql = source.readString();
    source.readMap(valuesForNewTasks, getClass().getClassLoader());
    name = source.readString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CustomFilterCriterion)) {
      return false;
    }
    CustomFilterCriterion that = (CustomFilterCriterion) o;
    return Objects.equals(valuesForNewTasks, that.valuesForNewTasks)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(text, that.text)
        && Objects.equals(sql, that.sql)
        && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valuesForNewTasks, identifier, text, sql, name);
  }
}
