/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.data;

import com.todoroo.andlib.sql.SqlTable;

/**
 * Table class. Most fields are final, so methods such as <code>as</code> will
 * clone the table when it returns.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class Table extends SqlTable {
    public final String name;
    public final Class<? extends AbstractModel> modelClass;

    public Table(String name, Class<? extends AbstractModel> modelClass) {
        this(name, modelClass, null);
    }

    public Table(String name, Class<? extends AbstractModel> modelClass, String alias) {
        super(name);
        this.name = name;
        this.alias = alias;
        this.modelClass = modelClass;
    }

    /**
     * Reads a list of properties from model class by reflection
     * @return property array
     */
    public Property<?>[] getProperties() {
        try {
            return (Property<?>[])modelClass.getField("PROPERTIES").get(null);
        } catch (IllegalArgumentException | NoSuchFieldException | IllegalAccessException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    // --- for sql-dsl

    /**
     * Create a new join table based on this table, but with an alias
     */
    @Override
    public Table as(String newAlias) {
        return new Table(name, modelClass, newAlias);
    }

    @Override
    public String toString() {
        if(hasAlias()) {
            return expression + " AS " + alias; //$NON-NLS-1$
        }
        return expression;
    }

    public String name() {
        if(hasAlias()) {
            return alias;
        }
        return name;
    }
}
