/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.SqlConstants.LEFT_PARENTHESIS;
import static com.todoroo.andlib.sql.SqlConstants.RIGHT_PARENTHESIS;
import static com.todoroo.andlib.sql.SqlConstants.SPACE;

import com.google.common.base.Joiner;
import java.util.List;

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

  public Criterion gt(Object value) {
    return UnaryCriterion.gt(this, value);
  }

  public Criterion gte(Object value) {
    return UnaryCriterion.gte(this, value);
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

  public <T> Criterion in(List<T> entries) {
    final Field field = this;
    return new Criterion(Operator.in) {
      @Override
      protected void populate(StringBuilder sb) {
        sb.append(field)
            .append(SPACE)
            .append(Operator.in)
            .append(SPACE)
            .append(LEFT_PARENTHESIS)
            .append(Joiner.on(",").join(entries))
            .append(RIGHT_PARENTHESIS);
      }
    };
  }
}
