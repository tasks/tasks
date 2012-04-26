package com.todoroo.astrid.data;

import android.content.ContentValues;
import android.net.Uri;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.Table;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.api.AstridApiConstants;

@SuppressWarnings("nls")
public class ABTestEvent extends AbstractModel {

    public static final long TEST_INTERVAL_0 = 0;
    public static final long TEST_INTERVAL_3 = 3 * DateUtilities.ONE_DAY;
    public static final long TEST_INTERVAL_7 = DateUtilities.ONE_WEEK;
    public static final long TEST_INTERVAL_14 = 2 * DateUtilities.ONE_WEEK;
    public static final long TEST_INTERVAL_21 = 3 * DateUtilities.ONE_WEEK;


    // --- table and uri

    /** table for this model */
    public static final Table TABLE = new Table("abtestevent", ABTestEvent.class);

    /** content uri for this model */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AstridApiConstants.PACKAGE + "/" +
            TABLE.name);


    // --- properties

    /** ID */
    public static final LongProperty ID = new LongProperty(
            TABLE, ID_PROPERTY_NAME);

    /** Name of the test -- one of the constant test keys defined in ABOptions */
    public static final StringProperty TEST_NAME = new StringProperty(
            TABLE, "testName");

    /**
     * Which variant (option) was chosen for this user --
     * one of the constants in the corresponding descriptions array in ABOptions
     */
    public static final StringProperty TEST_VARIANT = new StringProperty(
            TABLE, "testVariant");

    /**
     * Indicates if the user should be considered a new user for the purposes
     * of this test.
     * Should be 0 if no, 1 if yes
     */
    public static final IntegerProperty NEW_USER = new IntegerProperty(
            TABLE, "newUser"); // 0 if no, 1 if yes

    /**
     * Indicates if the user was "activated" at the time of recording this data
     * point.
     * Should be 0 if no, 1 if yes
     * Activated: 3 tasks created, one completed
     */
    public static final IntegerProperty ACTIVATED_USER = new IntegerProperty(
            TABLE, "activatedUser");

    /**
     * Which time interval event this data point corresponds to.
     * Should be one of the time interval constants defined above.
     */
    public static final LongProperty TIME_INTERVAL = new LongProperty(
            TABLE, "timeInterval"); // one of the constants defined above

    /** The actual date on which this data point was recorded. */
    public static final LongProperty DATE_RECORDED = new LongProperty(
            TABLE, "dateRecorded");

    /** List of all properties for this model */
    public static final Property<?>[] PROPERTIES = generateProperties(Task.class);

    private static final ContentValues defaultValues = new ContentValues();


    static {
        // initialize with default values
    }

    @Override
    public ContentValues getDefaultValues() {
        return defaultValues;
    }

    @Override
    public long getId() {
        return getIdHelper(ID);
    }

    // --- parcelable helpers

    public static final Creator<ABTestEvent> CREATOR = new ModelCreator<ABTestEvent>(ABTestEvent.class);

    @Override
    protected Creator<? extends AbstractModel> getCreator() {
        return CREATOR;
    }

}
