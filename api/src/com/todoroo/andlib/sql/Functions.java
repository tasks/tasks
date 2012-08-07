/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;




@SuppressWarnings("nls")
public final class Functions {

    public static String caseStatement(Criterion when, Object ifTrue, Object ifFalse) {
        return new StringBuilder("(CASE WHEN ").
            append(when.toString()).append(" THEN ").append(value(ifTrue)).
            append(" ELSE ").append(value(ifFalse)).append(" END)").toString();
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

    public static Field fromNow(long millis) {
        return new Field("(strftime('%s','now')*1000 + " + millis + ")");
    }

    public static Field strftime(LongProperty field, String format) {
        return new Field("(strftime('" + format + "', datetime(" + field.toString() + "/1000, 'unixepoch', 'localtime')))");
    }

    public static Field cast(Field field, String newType) {
        return new Field("CAST(" + field.toString() + " AS " +
                newType + ")");
    }

    public static Field max(Field field) {
        return new Field("MAX(" + field.toString() + ")");
    }

    public static Field count() {
        return new Field("COUNT(1)");
    }

    public static Field length(StringProperty field) {
        return new Field("LENGTH(" + field.toString() + ")");
    }

    public static Field bitwiseAnd(IntegerProperty field, int value) {
        return new Field(field.toString() + " & " + value);
    }


}
