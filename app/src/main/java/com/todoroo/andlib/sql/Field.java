/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.SqlConstants.COMMA;
import static com.todoroo.andlib.sql.SqlConstants.LEFT_PARENTHESIS;
import static com.todoroo.andlib.sql.SqlConstants.RIGHT_PARENTHESIS;
import static com.todoroo.andlib.sql.SqlConstants.SPACE;

public class Field extends DBObject<Field> {

  protected Field(String expression) {
    super(expression);
  }

  public static Field field(String expression) {
    return new Field(expression);
  }

  public Criterion eq(Object value) {
    if (value == null) {
      return UnaryCriterion.isNull(this);
    }
    return UnaryCriterion.eq(this, value);
  }

  public Criterion neq(Object value) {
    if (value == null) {
      return UnaryCriterion.isNotNull(this);
    }
    return UnaryCriterion.neq(this, value);
  }

  public Criterion gt(Object value) {
    return UnaryCriterion.gt(this, value);
  }

  public Criterion lt(final Object value) {
    return UnaryCriterion.lt(this, value);
  }

  public Criterion lte(final Object value) {
    return UnaryCriterion.lte(this, value);
  }

  public Criterion like(final String value) {
    return UnaryCriterion.like(this, value);
  }

  public <T> Criterion in(final Iterable<T> value) {
    final Field field = this;
    return new Criterion(Operator.in) {

      @Override
      protected void populate(StringBuilder sb) {
        sb.append(field)
            .append(SPACE)
            .append(Operator.in)
            .append(SPACE)
            .append(LEFT_PARENTHESIS)
            .append(SPACE);
        for (T t : value) {
          sb.append(t.toString()).append(COMMA);
        }
        sb.deleteCharAt(sb.length() - 1).append(RIGHT_PARENTHESIS);
      }
    };
  }

  public Criterion in(final Query query) {
    final Field field = this;
    return new Criterion(Operator.in) {

      @Override
      protected void populate(StringBuilder sb) {
        sb.append(field)
            .append(SPACE)
            .append(Operator.in)
            .append(SPACE)
            .append(LEFT_PARENTHESIS)
            .append(query)
            .append(RIGHT_PARENTHESIS);
      }
    };
  }
}
