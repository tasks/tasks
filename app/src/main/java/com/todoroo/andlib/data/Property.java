/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.data;

import android.text.TextUtils;
import com.todoroo.andlib.sql.Field;

/**
 * Property represents a typed column in a database.
 *
 * <p>Within a given database row, the parameter may not exist, in which case the value is null, it
 * may be of an incorrect type, in which case an exception is thrown, or the correct type, in which
 * case the value is returned.
 *
 * @param <TYPE> a database supported type, such as String or Integer
 * @author Tim Su <tim@todoroo.com>
 */
public abstract class Property<TYPE> extends Field implements Cloneable {

  // --- implementation

  /** The database column name for this property */
  public final String name;
  /** The database table name this property */
  private final Table table;

  /**
   * Create a property by table and column name. Uses the default property expression which is
   * derived from default table name
   */
  Property(Table table, String columnName) {
    this(table, columnName, (table == null) ? (columnName) : (table.name() + "." + columnName));
  }

  /** Create a property by table and column name, manually specifying an expression to use in SQL */
  Property(Table table, String columnName, String expression) {
    super(expression);
    this.table = table;
    this.name = columnName;
  }

  /** Return a clone of this property */
  @Override
  public Property<TYPE> clone() {
    try {
      return (Property<TYPE>) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /** Return a clone of this property */
  Property<TYPE> cloneAs(String tableAlias, String columnAlias) {
    Table aliasedTable = this.table;
    if (!TextUtils.isEmpty(tableAlias)) {
      aliasedTable = table.as(tableAlias);
    }

    try {
      Property<TYPE> newInstance =
          this.getClass()
              .getConstructor(Table.class, String.class)
              .newInstance(aliasedTable, this.name);
      if (!TextUtils.isEmpty(columnAlias)) {
        return (Property<TYPE>) newInstance.as(columnAlias);
      }
      return newInstance;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Integer property type. See {@link Property}
   *
   * @author Tim Su <tim@todoroo.com>
   */
  public static class IntegerProperty extends Property<Integer> {

    public IntegerProperty(Table table, String name) {
      super(table, name);
    }

    IntegerProperty(String name, String expression) {
      super(null, name, expression);
    }

    @Override
    public IntegerProperty as(String newAlias) {
      return (IntegerProperty) super.as(newAlias);
    }
  }

  /**
   * String property type. See {@link Property}
   *
   * @author Tim Su <tim@todoroo.com>
   */
  public static class StringProperty extends Property<String> {

    public StringProperty(Table table, String name) {
      super(table, name);
    }

    @Override
    public StringProperty as(String newAlias) {
      return (StringProperty) super.as(newAlias);
    }
  }

  /**
   * Long property type. See {@link Property}
   *
   * @author Tim Su <tim@todoroo.com>
   */
  public static class LongProperty extends Property<Long> {

    public LongProperty(Table table, String name) {
      super(table, name);
    }

    @Override
    public LongProperty cloneAs(String tableAlias, String columnAlias) {
      return (LongProperty) super.cloneAs(tableAlias, columnAlias);
    }
  }

  // --- pseudo-properties

  /** Runs a SQL function and returns the result as a string */
  public static class CountProperty extends IntegerProperty {

    public CountProperty() {
      super("count", "COUNT(1)");
      alias = "count";
    }
  }
}
