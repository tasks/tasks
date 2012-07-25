/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data;

/** Identifier of a single object. Extend this class to create your own */
public abstract class Identifier {
    private final long id;

    public Identifier(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String idAsString() {
        return Long.toString(id);
    }

    @Override
    public int hashCode() {
        return (int)id;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || o.getClass() != getClass())
            return false;

        return ((Identifier)o).getId() == getId();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + id; //$NON-NLS-1$
    }
}
