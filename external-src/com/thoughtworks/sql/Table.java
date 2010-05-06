package com.thoughtworks.sql;

public class Table extends DBObject<Table> {

    protected Table(String expression) {
        super(expression);
    }

    public static Table table(String table) {
        return new Table(table);
    }

    @SuppressWarnings("nls")
    protected String fieldExpression(String fieldName) {
        if (hasAlias()) {
            return alias + "." + fieldName;
        }
        return expression+"."+fieldName;
    }
}
