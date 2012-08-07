/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.SqlConstants.AND;
import static com.todoroo.andlib.sql.SqlConstants.JOIN;
import static com.todoroo.andlib.sql.SqlConstants.ON;
import static com.todoroo.andlib.sql.SqlConstants.SPACE;

public class Join {
    private final SqlTable joinTable;
    private final JoinType joinType;
    private final Criterion[] criterions;

    private Join(SqlTable table, JoinType joinType, Criterion... criterions) {
        joinTable = table;
        this.joinType = joinType;
        this.criterions = criterions;
    }

    public static Join inner(SqlTable expression, Criterion... criterions) {
        return new Join(expression, JoinType.INNER, criterions);
    }

    public static Join left(SqlTable table, Criterion... criterions) {
        return new Join(table, JoinType.LEFT, criterions);
    }

    public static Join right(SqlTable table, Criterion... criterions) {
        return new Join(table, JoinType.RIGHT, criterions);
    }

    public static Join out(SqlTable table, Criterion... criterions) {
        return new Join(table, JoinType.OUT, criterions);
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(joinType).append(SPACE).append(JOIN).append(SPACE).append(joinTable).append(SPACE).append(ON).append(SPACE).append("(");
        for (int i = 0; i < criterions.length; i++) {
            sb.append(criterions[i]);
            if (i < criterions.length - 1)
                sb.append(SPACE).append(AND).append(SPACE);
        }
        sb.append(")");
        return sb.toString();
    }
}
