/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.SqlConstants.AND;
import static com.todoroo.andlib.sql.SqlConstants.EXISTS;
import static com.todoroo.andlib.sql.SqlConstants.LEFT_PARENTHESIS;
import static com.todoroo.andlib.sql.SqlConstants.NOT;
import static com.todoroo.andlib.sql.SqlConstants.OR;
import static com.todoroo.andlib.sql.SqlConstants.RIGHT_PARENTHESIS;
import static com.todoroo.andlib.sql.SqlConstants.SPACE;

public abstract class Criterion {
    protected final Operator operator;

    public Criterion(Operator operator) {
        this.operator = operator;
    }

    public static Criterion all = new Criterion(Operator.exists) {
        @Override
        protected void populate(StringBuilder sb) {
            sb.append(1);
        }
    };

    public static Criterion none = new Criterion(Operator.exists) {
        @Override
        protected void populate(StringBuilder sb) {
            sb.append(0);
        }
    };

    public static Criterion and(final Criterion criterion, final Criterion... criterions) {
        return new Criterion(Operator.and) {

            @Override
            protected void populate(StringBuilder sb) {
                sb.append(criterion);
                for (Criterion c : criterions) {
                    sb.append(SPACE).append(AND).append(SPACE).append(c);
                }
            }
        };
    }

    public static Criterion or(final Criterion criterion, final Criterion... criterions) {
        return new Criterion(Operator.or) {

            @Override
            protected void populate(StringBuilder sb) {
                sb.append(criterion);
                for (Criterion c : criterions) {
                    sb.append(SPACE).append(OR).append(SPACE).append(c.toString());
                }
            }
        };
    }

    public static Criterion exists(final Query query) {
        return new Criterion(Operator.exists) {

            @Override
            protected void populate(StringBuilder sb) {
                sb.append(EXISTS).append(SPACE).append(LEFT_PARENTHESIS).append(query).append(RIGHT_PARENTHESIS);
            }
        };
    }

    public static Criterion not(final Criterion criterion) {
        return new Criterion(Operator.not) {

            @Override
            protected void populate(StringBuilder sb) {
                sb.append(NOT).append(SPACE);
                criterion.populate(sb);
            }
        };
    }

    protected abstract void populate(StringBuilder sb);

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(LEFT_PARENTHESIS);
        populate(builder);
        builder.append(RIGHT_PARENTHESIS);
        return builder.toString();
    }

}
