/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.SqlConstants.AS;
import static com.todoroo.andlib.sql.SqlConstants.SPACE;

import java.util.Objects;

public abstract class DBObject<T extends DBObject<?>> implements Cloneable {

  protected final String expression;
  protected String alias;

  DBObject(String expression) {
    this.expression = expression;
  }

  public T as(String newAlias) {
    try {
      T clone = (T) clone();
      clone.alias = newAlias;
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  protected boolean hasAlias() {
    return alias != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DBObject)) {
      return false;
    }
    DBObject<?> dbObject = (DBObject<?>) o;
    return Objects.equals(expression, dbObject.expression) && Objects.equals(alias, dbObject.alias);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expression, alias);
  }

  @Override
  public String toString() {
    if (hasAlias()) {
      return alias;
    }
    return expression;
  }

  public final String toStringInSelect() {
    StringBuilder sb = new StringBuilder(expression);
    if (hasAlias()) {
      sb.append(SPACE).append(AS).append(SPACE).append(alias);
    } else {
      int pos = expression.indexOf('.');
      if (pos > 0 && !expression.endsWith("*")) {
        sb.append(SPACE).append(AS).append(SPACE).append(expression.substring(pos + 1));
      }
    }
    return sb.toString();
  }
}
