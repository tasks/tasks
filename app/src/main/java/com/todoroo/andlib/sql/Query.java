/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.SqlConstants.ALL;
import static com.todoroo.andlib.sql.SqlConstants.COMMA;
import static com.todoroo.andlib.sql.SqlConstants.FROM;
import static com.todoroo.andlib.sql.SqlConstants.LIMIT;
import static com.todoroo.andlib.sql.SqlConstants.ORDER_BY;
import static com.todoroo.andlib.sql.SqlConstants.SELECT;
import static com.todoroo.andlib.sql.SqlConstants.SPACE;
import static com.todoroo.andlib.sql.SqlConstants.WHERE;
import static java.util.Arrays.asList;

import com.todoroo.astrid.data.Task;
import java.util.ArrayList;

public final class Query {

  private final ArrayList<Criterion> criterions = new ArrayList<>();
  private final ArrayList<Field> fields = new ArrayList<>();
  private final ArrayList<Join> joins = new ArrayList<>();
  private final ArrayList<Order> orders = new ArrayList<>();
  private SqlTable table;
  private String queryTemplate = null;
  private int limits = -1;

  private Query(Field... fields) {
    this.fields.addAll(asList(fields));
  }

  public static Query select() {
    return new Query(Task.PROPERTIES);
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

  public Query orderBy(Order... order) {
    orders.addAll(asList(order));
    return this;
  }

  public Query limit(int limit) {
    limits = limit;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    return this == o || !(o == null || getClass() != o.getClass()) && this.toString()
        .equals(o.toString());
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
    if (queryTemplate == null) {
      visitWhereClause(sql);
      visitOrderByClause(sql);
      visitLimitClause(sql);
    } else {
      if (orders.size() > 0) {
        throw new IllegalStateException("Can't have extras AND query template"); //$NON-NLS-1$
      }
      sql.append(queryTemplate);
    }

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

  private void visitLimitClause(StringBuilder sql) {
    if (limits > -1) {
      sql.append(LIMIT).append(SPACE).append(limits).append(SPACE);
    }
  }

  /**
   * Add the SQL query template (comes after the "from")
   *
   * @return query
   */
  public Query withQueryTemplate(String template) {
    queryTemplate = template;
    return this;
  }
}
