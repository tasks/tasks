package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.Constants.ALL;
import static com.todoroo.andlib.sql.Constants.COMMA;
import static com.todoroo.andlib.sql.Constants.FROM;
import static com.todoroo.andlib.sql.Constants.GROUP_BY;
import static com.todoroo.andlib.sql.Constants.LEFT_PARENTHESIS;
import static com.todoroo.andlib.sql.Constants.ORDER_BY;
import static com.todoroo.andlib.sql.Constants.RIGHT_PARENTHESIS;
import static com.todoroo.andlib.sql.Constants.SELECT;
import static com.todoroo.andlib.sql.Constants.SPACE;
import static com.todoroo.andlib.sql.Constants.WHERE;
import static com.todoroo.andlib.sql.SqlTable.table;
import static java.util.Arrays.asList;

import java.util.ArrayList;

import com.todoroo.andlib.data.Property;

public final class Query {

    private SqlTable table;
    private final ArrayList<Criterion> criterions = new ArrayList<Criterion>();
    private final ArrayList<Field> fields = new ArrayList<Field>();
    private final ArrayList<Join> joins = new ArrayList<Join>();
    private final ArrayList<Field> groupBies = new ArrayList<Field>();
    private final ArrayList<Order> orders = new ArrayList<Order>();
    private final ArrayList<Criterion> havings = new ArrayList<Criterion>();

    private Query(Field... fields) {
        this.fields.addAll(asList(fields));
    }

    public static Query select(Field... fields) {
        return new Query(fields);
    }

    public Query from(SqlTable fromTable) {
        this.table = fromTable;
        return this;
    }

    public Query join(Join... join) {
        joins.addAll(asList(join));
        return this;
    }

    public Query where(Criterion criterion) {
        criterions.add(criterion);
        return this;
    }

    public Query groupBy(Field... groupBy) {
        groupBies.addAll(asList(groupBy));
        return this;
    }

    public Query orderBy(Order... order) {
        orders.addAll(asList(order));
        return this;
    }

    public Query appendSelectFields(Property<?>... selectFields) {
        this.fields.addAll(asList(selectFields));
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && this.toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        visitSelectClause(sql);
        visitFromClause(sql);
        visitJoinClause(sql);
        visitWhereClause(sql);
        visitGroupByClause(sql);
        visitOrderByClause(sql);
        return sql.toString();
    }

    private void visitOrderByClause(StringBuilder sql) {
        if (orders.isEmpty()) {
            return;
        }
        sql.append(ORDER_BY);
        for (Order order : orders) {
            sql.append(SPACE).append(order).append(COMMA);
        }
        sql.deleteCharAt(sql.length() - 1).append(SPACE);
    }

    @SuppressWarnings("nls")
    private void visitGroupByClause(StringBuilder sql) {
        if (groupBies.isEmpty()) {
            return;
        }
        sql.append(GROUP_BY);
        for (Field groupBy : groupBies) {
            sql.append(SPACE).append(groupBy).append(COMMA);
        }
        sql.deleteCharAt(sql.length() - 1).append(SPACE);
        if (havings.isEmpty()) {
            return;
        }
        sql.append("HAVING");
        for (Criterion havingCriterion : havings) {
            sql.append(SPACE).append(havingCriterion).append(COMMA);
        }
        sql.deleteCharAt(sql.length() - 1).append(SPACE);
    }

    private void visitWhereClause(StringBuilder sql) {
        if (criterions.isEmpty()) {
            return;
        }
        sql.append(WHERE);
        for (Criterion criterion : criterions) {
            sql.append(SPACE).append(criterion).append(SPACE);
        }
    }

    private void visitJoinClause(StringBuilder sql) {
        for (Join join : joins) {
            sql.append(join).append(SPACE);
        }
    }

    private void visitFromClause(StringBuilder sql) {
        if (table == null) {
            return;
        }
        sql.append(FROM).append(SPACE).append(table).append(SPACE);
    }

    private void visitSelectClause(StringBuilder sql) {
        sql.append(SELECT).append(SPACE);
        if (fields.isEmpty()) {
            sql.append(ALL).append(SPACE);
            return;
        }
        for (Field field : fields) {
            sql.append(field.toStringInSelect()).append(COMMA);
        }
        sql.deleteCharAt(sql.length() - 1).append(SPACE);
    }

    public SqlTable as(String alias) {
        return table(LEFT_PARENTHESIS + this.toString() + RIGHT_PARENTHESIS).as(alias);
    }

    public Query having(Criterion criterion) {
        this.havings.add(criterion);
        return this;
    }

    /**
     * Gets a list of fields returned by this query
     * @return
     */
    public Property<?>[] getFields() {
        return fields.toArray(new Property<?>[fields.size()]);
    }
}
