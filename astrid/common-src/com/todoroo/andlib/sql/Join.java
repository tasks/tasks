package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.Constants.JOIN;
import static com.todoroo.andlib.sql.Constants.ON;
import static com.todoroo.andlib.sql.Constants.SPACE;

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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(joinType).append(SPACE).append(JOIN).append(SPACE).append(joinTable).append(SPACE).append(ON);
        for (Criterion criterion : criterions) {
            sb.append(SPACE).append(criterion);
        }
        return sb.toString();
    }
}
