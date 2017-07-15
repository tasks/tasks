/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

import com.todoroo.andlib.data.Property.StringProperty;




public final class Functions {

    public static String caseStatement(Criterion when, Object ifTrue, Object ifFalse) {
        return "(CASE WHEN " + when.toString() + " THEN " + value(ifTrue) + " ELSE " + value(ifFalse) + " END)";
    }

    private static String value(Object value) {
        return value.toString();
    }

    public static Field upper(Field title) {
        return new Field("UPPER(" + title.toString() + ")");
    }

    /**
     * @return SQL now (in milliseconds)
     */
    public static Field now() {
        return new Field("(strftime('%s','now')*1000)");
    }

    public static Field cast(Field field, String newType) {
        return new Field("CAST(" + field.toString() + " AS " +
                newType + ")");
    }

    public static Field length(StringProperty field) {
        return new Field("LENGTH(" + field.toString() + ")");
    }
}
