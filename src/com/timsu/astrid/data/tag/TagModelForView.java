package com.timsu.astrid.data.tag;

import android.database.Cursor;

import com.timsu.astrid.data.AbstractController;



/** Fields that you would want to see in the TaskView activity */
public class TagModelForView extends AbstractTagModel {

    static String[] FIELD_LIST = new String[] {
        AbstractController.KEY_ROWID,
        NAME,
    };

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
}
