/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.SqlConstants.SPACE;

public class UnaryCriterion extends Criterion {
    protected final Field expression;
    protected final Object value;

    UnaryCriterion(Field expression, Operator operator, Object value) {
        super(operator);
        this.expression = expression;
        this.value = value;
    }

    @Override
    protected void populate(StringBuilder sb) {
        beforePopulateOperator(sb);
        populateOperator(sb);
        afterPopulateOperator(sb);
    }

    public static Criterion eq(Field expression, Object value) {
        return new UnaryCriterion(expression, Operator.eq, value);
    }

    protected void beforePopulateOperator(StringBuilder sb) {
        sb.append(expression);
    }

    protected void populateOperator(StringBuilder sb) {
        sb.append(operator);
    }

    @SuppressWarnings("nls")
    protected void afterPopulateOperator(StringBuilder sb) {
        if(value == null)
            return;
        else if(value instanceof String)
            sb.append("'").append(sanitize((String) value)).append("'");
        else
            sb.append(value);
    }

    /**
     * Sanitize the given input for SQL
     * @param input
     * @return
     */
    @SuppressWarnings("nls")
    public static String sanitize(String input) {
        return input.replace("'", "''");
    }

    public static Criterion neq(Field field, Object value) {
        return new UnaryCriterion(field, Operator.neq, value);
    }

    public static Criterion gt(Field field, Object value) {
        return new UnaryCriterion(field, Operator.gt, value);
    }

    public static Criterion gte(Field field, Object value) {
        return new UnaryCriterion(field, Operator.gte, value);
    }

    public static Criterion lt(Field field, Object value) {
        return new UnaryCriterion(field, Operator.lt, value);
    }

    public static Criterion lte(Field field, Object value) {
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

    public static Criterion isNotNull(Field field) {
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

    public static Criterion like(Field field, String value, final String escape) {
        return new UnaryCriterion(field, Operator.like, value) {
            @Override
            protected void populateOperator(StringBuilder sb) {
                sb.append(SPACE).append(operator).append(SPACE);
            }
            @SuppressWarnings("nls")
            @Override
            protected void afterPopulateOperator(StringBuilder sb) {
                super.afterPopulateOperator(sb);
                sb.append(SPACE).append("ESCAPE").append(" '").append(sanitize(escape)).append("'");
            }
        };
    }
}
