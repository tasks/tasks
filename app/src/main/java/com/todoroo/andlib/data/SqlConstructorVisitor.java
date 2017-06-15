package com.todoroo.andlib.data;

/**
 * Visitor that returns SQL constructor for this property
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SqlConstructorVisitor implements Property.PropertyVisitor<String, Void> {

    @Override
    public String visitDouble(Property<Double> property, Void data) {
        return String.format("%s REAL", property.getColumnName());
    }

    @Override
    public String visitInteger(Property<Integer> property, Void data) {
        return String.format("%s INTEGER", property.getColumnName());
    }

    @Override
    public String visitLong(Property<Long> property, Void data) {
        return String.format("%s INTEGER", property.getColumnName());
    }

    @Override
    public String visitString(Property<String> property, Void data) {
        return String.format("%s TEXT", property.getColumnName());
    }
}

