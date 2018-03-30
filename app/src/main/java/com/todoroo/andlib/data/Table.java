/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.data;

import com.todoroo.andlib.sql.SqlTable;

/**
 * Table class. Most fields are final, so methods such as <code>as</code> will clone the table when
 * it returns.
 *
 * @author Tim Su <tim@todoroo.com>
 */
public final class Table extends SqlTable {

  private final String name;

  public Table(String name) {
    this(name, null);
  }

  private Table(String name, String alias) {
    super(name);
    this.name = name;
    this.alias = alias;
  }

  /** Create a new join table based on this table, but with an alias */
  @Override
  public Table as(String newAlias) {
    return new Table(name, newAlias);
  }

  @Override
  public String toString() {
    if (hasAlias()) {
      return expression + " AS " + alias; // $NON-NLS-1$
    }
    return expression;
  }

  public String name() {
    if (hasAlias()) {
      return alias;
    }
    return name;
  }
}
