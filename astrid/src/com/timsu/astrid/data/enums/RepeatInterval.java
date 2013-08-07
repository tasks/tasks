/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.timsu.astrid.data.enums;

import java.util.Date;

public enum RepeatInterval {

    DAYS(0) {
        @Override
        public void offsetDateBy(Date input, int number) {
            input.setDate(input.getDate() + number);
        }
    },
    WEEKS(0) {
        @Override
        public void offsetDateBy(Date input, int number) {
            input.setDate(input.getDate() + 7 * number);
        }
    },
    MONTHS(0) {
        @Override
        public void offsetDateBy(Date input, int number) {
            input.setMonth(input.getMonth() + number);
        }
    },
    HOURS(0) {
        @Override
        public void offsetDateBy(Date input, int number) {
            input.setHours(input.getHours() + number);
        }
    },;

    int label;

    private RepeatInterval(int label) {
        this.label = label;
    }

    public int getLabelResource() {
        return label;
    }

    abstract public void offsetDateBy(Date input, int number);
}
