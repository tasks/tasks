package com.todoroo.andlib.sql;

import java.util.List;
import java.util.ArrayList;

public class GroupBy {
    private List<Field> fields = new ArrayList<Field>();

    public static GroupBy groupBy(Field field) {
        GroupBy groupBy = new GroupBy();
        groupBy.fields.add(field);
        return groupBy;
    }
}
