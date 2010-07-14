package com.todoroo.astrid.dao;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.astrid.model.Metadata;

/**
 * Helper class for representing string columns from joined metadata
 */
public class IntegerJoinProperty extends IntegerProperty implements JoinProperty {

    public IntegerJoinProperty(String name) {
        super(null, name);
    }

    @SuppressWarnings("nls")
    public String joinTable() {
        return String.format("SELECT %s,%s AS %s FROM %s WHERE %s='%s'",
                Metadata.TASK, Metadata.VALUE, name,
                Metadata.TABLE, Metadata.KEY, name);
    }

}