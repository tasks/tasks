package com.todoroo.astrid.repeats;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

import static com.todoroo.astrid.repeats.RepeatTaskCompleteListener.handleSubdayRepeat;
import static org.junit.Assert.assertEquals;
import static org.tasks.date.DateTimeUtils.newDate;

@RunWith(RobolectricTestRunner.class)
public class RepeatTaskCompleteListenerTest {
    private static final Date start = newDate(2013, 12, 31, 8, 31, 59);

    @Test
    public void repeatEveryMinute() {
        assertEquals(
                newDate(2013, 12, 31, 8, 32, 1).getTime(), // astrid always sets seconds to one
                handleSubdayRepeat(start, newRule(1, Frequency.MINUTELY)));
    }

    @Test
    public void repeatEveryOtherMinute() {
        assertEquals(
                newDate(2013, 12, 31, 8, 33, 1).getTime(),
                handleSubdayRepeat(start, newRule(2, Frequency.MINUTELY)));
    }

    @Test
    public void repeatEveryHour() {
        assertEquals(
                newDate(2013, 12, 31, 9, 31, 1).getTime(),
                handleSubdayRepeat(start, newRule(1, Frequency.HOURLY)));
    }

    @Test
    public void repeatEveryOtherHour() {
        assertEquals(
                newDate(2013, 12, 31, 10, 31, 1).getTime(),
                handleSubdayRepeat(start, newRule(2, Frequency.HOURLY)));
    }

    private RRule newRule(int interval, Frequency frequency) {
        RRule rule = new RRule();
        rule.setInterval(interval);
        rule.setFreq(frequency);
        return rule;
    }
}
