/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Metadata;

/**
 * Metadata entries for a GTasks Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksMetadata {

    static final int VALUE_UNSET = -1;

    /** metadata key */
    public static final String METADATA_KEY = "gtasks"; //$NON-NLS-1$

    /** task id in google */
    public static final StringProperty ID = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    public static final StringProperty LIST_ID = new StringProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** parent task id, or 0 if top level task */
    public static final LongProperty PARENT_TASK = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    public static final IntegerProperty INDENT = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

    public static final LongProperty ORDER = new LongProperty(Metadata.TABLE,
            Metadata.VALUE5.name);

    public static final LongProperty GTASKS_ORDER = new LongProperty(Metadata.TABLE,
            Metadata.VALUE6.name);

    public static final LongProperty LAST_SYNC = new LongProperty(Metadata.TABLE,
            Metadata.VALUE7.name);

    /**
     * Creates default GTasks metadata item
     * @param taskId if > 0, will set metadata task field
     * @return
     */
    public static Metadata createEmptyMetadata(long taskId) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, GtasksMetadata.METADATA_KEY);
        metadata.setValue(ID, ""); //$NON-NLS-1$

        String defaultList = Preferences.getStringValue(GtasksPreferenceService.PREF_DEFAULT_LIST);
        if(defaultList == null)
            defaultList = "@default"; //$NON-NLS-1$

        metadata.setValue(LIST_ID, defaultList);
        metadata.setValue(PARENT_TASK, AbstractModel.NO_ID);
        metadata.setValue(INDENT, 0);
        metadata.setValue(ORDER, DateUtilities.now());
        if(taskId > AbstractModel.NO_ID)
            metadata.setValue(Metadata.TASK, taskId);
        return metadata;
    }

}
