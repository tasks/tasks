/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.SqlConstants.ALL;
import static com.todoroo.andlib.sql.SqlConstants.COMMA;
import static com.todoroo.andlib.sql.SqlConstants.FROM;
import static com.todoroo.andlib.sql.SqlConstants.SELECT;
import static com.todoroo.andlib.sql.SqlConstants.SPACE;
import static com.todoroo.andlib.sql.SqlConstants.WHERE;
import static java.util.Arrays.asList;

import java.util.ArrayList;

public final class Query {

  private final ArrayList<Criterion> criterions = new ArrayList<>();
  private final ArrayList<Field> fields = new ArrayList<>();
  private final ArrayList<Join> joins = new ArrayList<>();
  private SqlTable table;
  private String queryTemplate = null;
  private String preClause = null;

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

  @Override
  public boolean equals(Object o) {
    return this == o
        || !(o == null || getClass() != o.getClass()) && this.toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sql = new StringBuilder();
    if (preClause != null) {
      sql.append(preClause);
    }
    visitSelectClause(sql);
    visitFromClause(sql);

    visitJoinClause(sql);
    if (queryTemplate == null) {
      visitWhereClause(sql);
    } else {
      sql.append(queryTemplate);
    }

    return sql.toString();
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

  /**
   * Add the SQL query template (comes after the "from")
   *
   * @return query
   */
  public Query withQueryTemplate(String template) {
    queryTemplate = template;
    return this;
  }

  public Query withPreClause(String clause) {
    preClause = clause;
    return this;
  }
}
