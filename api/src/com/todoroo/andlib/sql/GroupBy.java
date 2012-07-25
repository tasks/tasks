/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

import java.util.ArrayList;
import java.util.List;

public class GroupBy {
    private List<Field> fields = new ArrayList<Field>();

    public static GroupBy groupBy(Field field) {
        GroupBy groupBy = new GroupBy();
        groupBy.fields.add(field);
        return groupBy;
    }
}
