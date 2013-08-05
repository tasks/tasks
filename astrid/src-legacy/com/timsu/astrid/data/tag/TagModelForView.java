/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.tag;

import android.database.Cursor;

import com.timsu.astrid.data.LegacyAbstractController;


/**
 * Tag model for viewing purposes. Contains task name
 */

public class TagModelForView extends AbstractTagModel {

    static String[] FIELD_LIST = new String[]{
            LegacyAbstractController.KEY_ROWID,
            NAME,
    };

    // negative number, should not conflict with database row #'s
    public static final TagIdentifier UNTAGGED_IDENTIFIER = new TagIdentifier(Long.MIN_VALUE);
    public static final String UNTAGGED_DEFAULT_NAME = "[untagged]";
    private static TagModelForView UNTAGGED_TASKS = new TagModelForView(UNTAGGED_DEFAULT_NAME);

    public static final String HIDDEN_FROM_MAIN_LIST_PREFIX = "_";

    // --- constructors

    /**
     * Constructor for creating a new model
     */
    TagModelForView(String name) {
        setName(name);
    }

    /**
     * Constructor for getting an existing model
     */
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
}
