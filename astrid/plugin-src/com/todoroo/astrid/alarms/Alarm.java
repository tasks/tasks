/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.alarms;


import android.content.ContentValues;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.astrid.model.Task;

/**
 * Data Model which represents an alarm
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class Alarm extends AbstractModel {

    // --- table

    public static final Table TABLE = new Table("alarm", Alarm.class);

    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Associated Task */
    public static final LongProperty TASK = new LongProperty(
            TABLE, "task");

    /** Alarm Time */
    public static final LongProperty TIME = new LongProperty(
            TABLE, "time");

    /** Alarm Type (see constants) */
    public static final IntegerProperty TYPE = new IntegerProperty(
            TABLE, "type");

    /** Alarm Ringtone */
    public static final StringProperty RINGTONE = new StringProperty(
            TABLE, "ringtone");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(Alarm.class);

    // --- constants

    /** this alarm was already triggered */
    public static final int TYPE_TRIGGERED = 0;

    /** this alarm is single-shot */
    public static final int TYPE_SINGLE = 1;

    /** this alarm repeats itself until turned off */
    public static final int TYPE_REPEATING = 2;

    // --- defaults

    /** Default values container */
    private static final ContentValues defaultValues = new ContentValues();

    static {
        defaultValues.put(TYPE.name, TYPE_SINGLE);
        defaultValues.put(RINGTONE.name, "");
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    // --- data access boilerplate

    public Alarm() {
        super();
    }

    public Alarm(TodorooCursor<Alarm> cursor) {
        this();
        readPropertiesFromCursor(cursor);
    }

    public void readFromCursor(TodorooCursor<Alarm> cursor) {
        super.readPropertiesFromCursor(cursor);
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    };

    // --- parcelable helpers

    private static final Creator<Task> CREATOR = new ModelCreator<Task>(Task.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
