package com.todoroo.astrid.producteev;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.producteev.sync.ProducteevTaskContainer;
import com.todoroo.astrid.rmilk.data.MilkList;

/**
 * Metadata entries for a Remember The Milk Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ProducteevTask {

    /** metadata key */
    public static final String METADATA_KEY = "producteev"; //$NON-NLS-1$

    /** {@link MilkList} id */
    public static final LongProperty ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** RTM Task Series Id */
    public static final LongProperty DASHBOARD_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /**
     * Creates a piece of metadata from a remote task
     * @param rtmTaskSeries
     * @return
     */
    public static Metadata create(ProducteevTaskContainer container) {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, METADATA_KEY);
        metadata.setValue(ProducteevTask.ID, container.id);

        return metadata;
    }

}
