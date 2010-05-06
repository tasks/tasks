package com.thoughtworks.sql;

import static com.thoughtworks.sql.Constants.SPACE;
import static com.thoughtworks.sql.Constants.JOIN;
import static com.thoughtworks.sql.Constants.ON;

public class Join {
    private final Table joinTable;
    private final JoinType joinType;
    private final Criterion[] criterions;

    private Join(Table table, JoinType joinType, Criterion... criterions) {
        joinTable = table;
        this.joinType = joinType;
        this.criterions = criterions;
    }

    public static Join inner(Table expression, Criterion... criterions) {
        return new Join(expression, JoinType.INNER, criterions);
    }

    public static Join left(Table table, Criterion... criterions) {
        return new Join(table, JoinType.LEFT, criterions);
    }

    public static Join right(Table table, Criterion... criterions) {
        return new Join(table, JoinType.RIGHT, criterions);
    }

    public static Join out(Table table, Criterion... criterions) {
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
