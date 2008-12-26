/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.data;

/** Identifier of a single object. Extend this class to create your own */
public abstract class Identifier {
    private long id;

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
        return getClass().getSimpleName() + ": " + id;
    }
}
