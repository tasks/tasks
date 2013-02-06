/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

import static com.todoroo.andlib.sql.SqlConstants.ALL;
import static com.todoroo.andlib.sql.SqlConstants.COMMA;
import static com.todoroo.andlib.sql.SqlConstants.DISTINCT;
import static com.todoroo.andlib.sql.SqlConstants.FROM;
import static com.todoroo.andlib.sql.SqlConstants.GROUP_BY;
import static com.todoroo.andlib.sql.SqlConstants.LEFT_PARENTHESIS;
import static com.todoroo.andlib.sql.SqlConstants.LIMIT;
import static com.todoroo.andlib.sql.SqlConstants.ORDER_BY;
import static com.todoroo.andlib.sql.SqlConstants.RIGHT_PARENTHESIS;
import static com.todoroo.andlib.sql.SqlConstants.SELECT;
import static com.todoroo.andlib.sql.SqlConstants.SPACE;
import static com.todoroo.andlib.sql.SqlConstants.UNION;
import static com.todoroo.andlib.sql.SqlConstants.WHERE;
import static com.todoroo.andlib.sql.SqlTable.table;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import com.todoroo.andlib.data.Property;
import com.todoroo.astrid.api.AstridApiConstants;

public final class Query {

    private SqlTable table;
    private String queryTemplate = null;
    private final ArrayList<Criterion> criterions = new ArrayList<Criterion>();
    private final ArrayList<Field> fields = new ArrayList<Field>();
    private final ArrayList<Join> joins = new ArrayList<Join>();
    private final ArrayList<Field> groupBies = new ArrayList<Field>();
    private final ArrayList<Query> unions = new ArrayList<Query>();
    private final ArrayList<Order> orders = new ArrayList<Order>();
    private final ArrayList<Criterion> havings = new ArrayList<Criterion>();
    private int limits = -1;
    private boolean distinct = false;

    private Query(Field... fields) {
        this.fields.addAll(asList(fields));
    }

    public static Query select(Field... fields) {
        return new Query(fields);
    }

    public static Query selectDistinct(Field... fields) {
        Query query = new Query(fields);
        query.distinct = true;
        return query;
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

    public Query union(Query query) {
        unions.add(query);
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
        if(queryTemplate == null) {
            visitWhereClause(sql);
            visitGroupByClause(sql);
            visitUnionClause(sql);
            visitOrderByClause(sql);
            visitLimitClause(sql);
        } else {
            if(groupBies.size() > 0 || orders.size() > 0 ||
                    havings.size() > 0)
                throw new IllegalStateException("Can't have extras AND query template"); //$NON-NLS-1$
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

    private void visitUnionClause(StringBuilder sql) {
        if (unions.isEmpty()) {
            return;
        }
        for (Query query : unions) {
            sql.append(UNION).append(SPACE).append(query).append(SPACE);
        }
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
        if(distinct)
            sql.append(DISTINCT).append(SPACE);
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
        if(limits > -1)
            sql.append(LIMIT).append(SPACE).append(limits).append(SPACE);
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

    /**
     * Add the SQL query template (comes after the "from")
     * @param template
     * @return query
     */
    public Query withQueryTemplate(String template) {
        queryTemplate = template;
        return this;
    }

    /**
     * Parse out properties and run query
     * @param cr
     * @param baseUri
     * @return
     */
    public Cursor queryContentResolver(ContentResolver cr, Uri baseUri) {
        Uri uri = baseUri;

        if(joins.size() != 0)
            throw new UnsupportedOperationException("can't perform join in content resolver query"); //$NON-NLS-1$

        String[] projection = new String[fields.size()];
        for(int i = 0; i < projection.length; i++)
            projection[i] = fields.get(i).toString();

        StringBuilder groupByClause = new StringBuilder();
        StringBuilder selectionClause = new StringBuilder();
        StringBuilder orderClause = new StringBuilder();
        if(queryTemplate != null) {
            QueryTemplateHelper.queryForContentResolver(queryTemplate,
                    selectionClause, orderClause, groupByClause);
        } else {
            if(groupBies.size() > 0) {
                for (Field groupBy : groupBies)
                    groupByClause.append(SPACE).append(groupBy).append(COMMA);
                if(groupByClause.length() > 0)
                    groupByClause.deleteCharAt(groupByClause.length() - 1);
            }

            for (Criterion criterion : criterions)
                selectionClause.append(criterion).append(SPACE);

            for (Order order : orders)
                orderClause.append(SPACE).append(order).append(COMMA);
            if(orderClause.length() > 0)
                orderClause.deleteCharAt(orderClause.length() - 1);
        }

        if(groupByClause.length() > 0)
            uri = Uri.withAppendedPath(baseUri, AstridApiConstants.GROUP_BY_URI +
                    groupByClause.toString().trim());
        return cr.query(uri, projection, selectionClause.toString(), null,
                orderClause.toString());
    }

    /** query template helper */
    public static class QueryTemplateHelper {

        /** build a content resolver query */
        @SuppressWarnings("nls")
        public static void queryForContentResolver(String queryTemplate,
                StringBuilder selectionClause, StringBuilder orderClause,
                StringBuilder groupByClause) {

            Pattern where = Pattern.compile("WHERE (.*?)(LIMIT|HAVING|GROUP|ORDER|\\Z)");
            Matcher whereMatcher = where.matcher(queryTemplate);
            if(whereMatcher.find())
                selectionClause.append(whereMatcher.group(1).trim());

            Pattern group = Pattern.compile("GROUP BY (.*?)(LIMIT|HAVING|ORDER|\\Z)");
            Matcher groupMatcher = group.matcher(queryTemplate);
            if(groupMatcher.find())
                groupByClause.append(groupMatcher.group(1).trim());

            Pattern order = Pattern.compile("ORDER BY (.*?)(LIMIT|HAVING|\\Z)");
            Matcher orderMatcher = order.matcher(queryTemplate);
            if(orderMatcher.find())
                orderClause.append(orderMatcher.group(1).trim());
        }

    }

}
