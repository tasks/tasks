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
package com.timsu.astrid.data.tag;

import android.database.Cursor;

import com.timsu.astrid.data.AbstractController;


/** Tag model for viewing purposes. Contains task name */
public class TagModelForView extends AbstractTagModel {

    static String[] FIELD_LIST = new String[] {
        AbstractController.KEY_ROWID,
        NAME,
    };

    public static TagModelForView UNTAGGED_TASKS = new TagModelForView("[untagged]");
    public static String HIDDEN_FROM_MAIN_LIST_PREFIX = "_";

    // --- constructors

    /** Constructor for creating a new model */
    TagModelForView(String name) {
        super();
        setName(name);
    }

    /** Constructor for getting an existing model */
    TagModelForView(Cursor cursor) {
        super(cursor);
        getName();
    }

    // --- getters and setters

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean shouldHideFromMainList() {
        return getName().startsWith(HIDDEN_FROM_MAIN_LIST_PREFIX);
    }

    public void toggleHideFromMainList() {
        if(shouldHideFromMainList())
            setName(getName().substring(HIDDEN_FROM_MAIN_LIST_PREFIX.length()));
        else
            setName(HIDDEN_FROM_MAIN_LIST_PREFIX + getName());
    }
}
