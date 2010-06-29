/*
 * Copyright (c) 2009, Todoroo Inc
 * All Rights Reserved
 * http://www.todoroo.com
 */
package com.todoroo.andlib.data;

import com.todoroo.andlib.sql.Field;

/**
 * Property represents a typed column in a database.
 *
 * Within a given database row, the parameter may not exist, in which case the
 * value is null, it may be of an incorrect type, in which case an exception is
 * thrown, or the correct type, in which case the value is returned.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 * @param <TYPE>
 *            a database supported type, such as String or Integer
 */
@SuppressWarnings("nls")
public abstract class Property<TYPE> extends Field implements Cloneable {

    // --- implementation

    /** The database table name this property */
    public final Table table;

    /** The database column name for this property */
    public final String name;

    /**
     * Create a property by table and column name. Uses the default property
     * expression which is derived from default table name
     */
    protected Property(Table table, String columnName) {
        this(table, columnName, (table == null) ? (columnName) : (table.name + "." + columnName));
    }

    /**
     * Create a property by table and column name, manually specifying an
     * expression to use in SQL
     */
    protected Property(Table table, String columnName, String expression) {
        super(expression);
        this.table = table;
        this.name = columnName;
    }

    /**
     * Accept a visitor
     */
    abstract public <RETURN, PARAMETER> RETURN accept(
            PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data);

    /**
     * Return a clone of this property
     */
    @Override
    public Property<TYPE> clone() {
        try {
            return (Property<TYPE>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    // --- helper classes and interfaces

    /**
     * Visitor interface for property classes
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public interface PropertyVisitor<RETURN, PARAMETER> {
        public RETURN visitInteger(Property<Integer> property, PARAMETER data);

        public RETURN visitLong(Property<Long> property, PARAMETER data);

        public RETURN visitDouble(Property<Double> property, PARAMETER data);

        public RETURN visitString(Property<String> property, PARAMETER data);
    }

    // --- children

    /**
     * Integer property type. See {@link Property}
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class IntegerProperty extends Property<Integer> {

        public IntegerProperty(Table table, String name) {
            super(table, name);
        }

        protected IntegerProperty(Table table, String name, String expression) {
            super(table, name, expression);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(
                PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitInteger(this, data);
        }
    }

    /**
     * String property type. See {@link Property}
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class StringProperty extends Property<String> {

        public StringProperty(Table table, String name) {
            super(table, name);
        }

        protected StringProperty(Table table, String name, String expression) {
            super(table, name, expression);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(
                PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitString(this, data);
        }
    }

    /**
     * Double property type. See {@link Property}
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class DoubleProperty extends Property<Double> {

        public DoubleProperty(Table table, String name) {
            super(table, name);
        }

        protected DoubleProperty(Table table, String name, String expression) {
            super(table, name, expression);
        }


        @Override
        public <RETURN, PARAMETER> RETURN accept(
                PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitDouble(this, data);
        }
    }

    /**
     * Long property type. See {@link Property}
     *
     * @author Tim Su <tim@todoroo.com>
     *
     */
    public static class LongProperty extends Property<Long> {

        public LongProperty(Table table, String name) {
            super(table, name);
        }

        protected LongProperty(Table table, String name, String expression) {
            super(table, name, expression);
        }

        @Override
        public <RETURN, PARAMETER> RETURN accept(
                PropertyVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
            return visitor.visitLong(this, data);
        }
    }

    // --- pseudo-properties

    /** Runs a SQL function and returns the result as a string */
    public static class StringFunctionProperty extends StringProperty {
        public StringFunctionProperty(String function, String columnName) {
            super(null, columnName, function);
            alias = columnName;
        }
    }

    /** Runs a SQL function and returns the result as a string */
    public static class IntegerFunctionProperty extends IntegerProperty {
        public IntegerFunctionProperty(String function, String columnName) {
            super(null, columnName, function);
            alias = columnName;
        }
    }

    /** Counting in aggregated tables. Returns the result of COUNT(1) */
    public static final class CountProperty extends IntegerFunctionProperty {
        public CountProperty() {
            super("COUNT(1)", "count");
        }
    }

}
