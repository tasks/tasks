/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.sql;

public class EqCriterion extends UnaryCriterion {
    EqCriterion(Field field, Object value) {
        super(field, Operator.eq, value);
    }
}
