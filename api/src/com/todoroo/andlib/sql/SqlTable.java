/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

public class SqlTable extends DBObject<SqlTable> {

    protected SqlTable(String expression) {
        super(expression);
    }

    public static SqlTable table(String table) {
        return new SqlTable(table);
    }

    @SuppressWarnings("nls")
    protected String fieldExpression(String fieldName) {
        if (hasAlias()) {
            return alias + "." + fieldName;
        }
        return expression+"."+fieldName;
    }
}
