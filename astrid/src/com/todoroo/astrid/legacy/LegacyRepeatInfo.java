/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.legacy;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;

/** Legacy repeatInfo class */
public class LegacyRepeatInfo {
    public static final int REPEAT_VALUE_OFFSET    = 3;

    /** Legacy repeat interval class */
    public enum LegacyRepeatInterval {
        DAYS,
        WEEKS,
        MONTHS,
        HOURS
    }

    private final LegacyRepeatInterval interval;
    private final int value;

    public LegacyRepeatInfo(LegacyRepeatInterval repeatInterval, int value) {
        this.interval = repeatInterval;
        this.value = value;
    }

    public LegacyRepeatInterval getInterval() {
        return interval;
    }

    public int getValue() {
        return value;
    }

    public static LegacyRepeatInfo fromSingleField(int repeat) {
        if(repeat == 0)
            return null;
        int value = repeat >> REPEAT_VALUE_OFFSET;
        LegacyRepeatInterval interval = LegacyRepeatInterval.values()
        [repeat - (value << REPEAT_VALUE_OFFSET)];

        return new LegacyRepeatInfo(interval, value);
    }

    public RRule toRRule() {
        RRule rrule = new RRule();
        rrule.setInterval(getValue());
        switch(getInterval()) {
        case DAYS:
            rrule.setFreq(Frequency.DAILY);
            break;
        case WEEKS:
            rrule.setFreq(Frequency.WEEKLY);
            break;
        case MONTHS:
            rrule.setFreq(Frequency.MONTHLY);
            break;
        case HOURS:
            rrule.setFreq(Frequency.HOURLY);
        }
        return rrule;
    }

}
