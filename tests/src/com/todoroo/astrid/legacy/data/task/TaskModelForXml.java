/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.legacy.data.task;

import java.util.Date;
import java.util.HashMap;

import android.database.Cursor;

import com.todoroo.astrid.legacy.data.AbstractController;
import com.todoroo.astrid.legacy.data.enums.RepeatInterval;

public class TaskModelForXml extends AbstractTaskModel {

    static String[] FIELD_LIST = new String[] {
            AbstractController.KEY_ROWID,
            NAME,
            IMPORTANCE,
            ELAPSED_SECONDS,
            ESTIMATED_SECONDS,
            TIMER_START,
            DEFINITE_DUE_DATE,
            PREFERRED_DUE_DATE,
            NOTIFICATIONS,
            PROGRESS_PERCENTAGE,
            COMPLETION_DATE,
            CREATION_DATE,
            HIDDEN_UNTIL,
            NOTES,
            REPEAT,
            FLAGS,
            POSTPONE_COUNT,
            BLOCKING_ON,
            LAST_NOTIFIED,
            NOTIFICATION_FLAGS,
            CALENDAR_URI,
    };
    private HashMap<String, String> taskAttributesMap;
    public static final String REPEAT_VALUE = "repeat_value";
    public static final String REPEAT_INTERVAL = "repeat_interval";


    private RepeatInterval repeatInterval = null;
    private Integer repeatValue = null;

    // --- constructors

    public TaskModelForXml() {
        super();
        setCreationDate(new Date());
        taskAttributesMap = new HashMap<String, String>(FIELD_LIST.length);
    }

    public TaskModelForXml(Cursor cursor) {
        super(cursor);
        prefetchData(FIELD_LIST);
        taskAttributesMap = new HashMap<String, String>(FIELD_LIST.length);
    }

    // --- getters and setters

    @Override
    public Date getCreationDate() {
        return super.getCreationDate();
    }

    /* Build a HashMap of task fields and associated values.
     */
    public HashMap<String, String> getTaskAttributes() {

        return taskAttributesMap;
    }

    // --- setters

    public boolean setField(String field, String value) {

        return false;
    }

    public void setRepeatInterval(RepeatInterval repeatInterval) {
        this.repeatInterval = repeatInterval;
        if (repeatValue != null) {
            setRepeat(new RepeatInfo(repeatInterval, repeatValue));
        }
    }

    public void setRepeatValue(Integer repeatValue) {
        this.repeatValue = repeatValue;
        if (repeatInterval != null) {
            setRepeat(new RepeatInfo(repeatInterval, repeatValue));
        }
    }
}
