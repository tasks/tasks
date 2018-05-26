/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.SqlConstants.SPACE;

public class UnaryCriterion extends Criterion {

  private final Field expression;
  private final Object value;

  private UnaryCriterion(Field expression, Operator operator, Object value) {
    super(operator);
    this.expression = expression;
    this.value = value;
  }

  public static Criterion eq(Field expression, Object value) {
    return new UnaryCriterion(expression, Operator.eq, value);
  }

  /** Sanitize the given input for SQL */
  public static String sanitize(String input) {
    return input.replace("'", "''");
  }

  static Criterion neq(Field field, Object value) {
    return new UnaryCriterion(field, Operator.neq, value);
  }

  static Criterion gt(Field field, Object value) {
    return new UnaryCriterion(field, Operator.gt, value);
  }

  static Criterion lt(Field field, Object value) {
    return new UnaryCriterion(field, Operator.lt, value);
  }

  static Criterion lte(Field field, Object value) {
    return new UnaryCriterion(field, Operator.lte, value);
  }

  public static Criterion isNull(Field field) {
    return new UnaryCriterion(field, Operator.isNull, null) {
      @Override
      protected void populateOperator(StringBuilder sb) {
        sb.append(SPACE).append(operator);
      }
    };
  }

  static Criterion isNotNull(Field field) {
    return new UnaryCriterion(field, Operator.isNotNull, null) {
      @Override
      protected void populateOperator(StringBuilder sb) {
        sb.append(SPACE).append(operator);
      }
    };
  }

  public static Criterion like(Field field, String value) {
    return new UnaryCriterion(field, Operator.like, value) {
      @Override
      protected void populateOperator(StringBuilder sb) {
        sb.append(SPACE).append(operator).append(SPACE);
      }
    };
  }

  @Override
  protected void populate(StringBuilder sb) {
    beforePopulateOperator(sb);
    populateOperator(sb);
    afterPopulateOperator(sb);
  }

  @SuppressWarnings("WeakerAccess")
  void beforePopulateOperator(StringBuilder sb) {
    sb.append(expression);
  }

  @SuppressWarnings("WeakerAccess")
  void populateOperator(StringBuilder sb) {
    sb.append(operator);
  }

  @SuppressWarnings("WeakerAccess")
  void afterPopulateOperator(StringBuilder sb) {
    if (value == null) {
      return;
    }

    if (value instanceof String) {
      sb.append("'").append(sanitize((String) value)).append("'");
    } else {
      sb.append(value);
    }
  }
}
