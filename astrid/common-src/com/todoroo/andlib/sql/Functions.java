package com.todoroo.andlib.sql;

@SuppressWarnings("nls")
public final class Functions {

    public static String caseStatement(Criterion when, Object ifTrue, Object ifFalse) {
        return new StringBuilder("CASE WHEN ").
            append(when.toString()).append(" THEN ").append(value(ifTrue)).
            append(" ELSE ").append(value(ifFalse)).append(" END").toString();
    }

    private static String value(Object value) {
        return value.toString();
    }

}
