package com.todoroo.andlib.sql;




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

    public static Field cast(Field field, String newType) {
        return new Field("CAST(" + field.toString() + " AS " +
                newType + ")");
    }

    public static Field max(Field field) {
        return new Field("MAX(" + field.toString() + ")");
    }

}
