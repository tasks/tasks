/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import java.util.ArrayList;
import java.util.Iterator;

import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.sync.SyncContainer;

/**
 * RTM Task Container
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksTaskContainer extends SyncContainer {

    public Metadata gtaskMetadata;

    // position information
    public String parentId = null;
    public String priorSiblingId = null;

    public GtasksTaskContainer(Task task, ArrayList<Metadata> metadata, Metadata gtaskMetadata) {
        this.task = task;
        this.metadata = metadata;
        this.gtaskMetadata = gtaskMetadata;
        if(this.gtaskMetadata == null) {
            this.gtaskMetadata = GtasksMetadata.createEmptyMetadata(task.getId());
        }
    }

    public GtasksTaskContainer(Task task, ArrayList<Metadata> metadata) {
        this.task = task;
        this.metadata = metadata;

        for(Iterator<Metadata> iterator = metadata.iterator(); iterator.hasNext(); ) {
            Metadata item = iterator.next();
            if(GtasksMetadata.METADATA_KEY.equals(item.getValue(Metadata.KEY))) {
                gtaskMetadata = item;
                iterator.remove();
                // don't break, could be multiple
            }
        }
        if(this.gtaskMetadata == null) {
            this.gtaskMetadata = GtasksMetadata.createEmptyMetadata(task.getId());
        }
    }

    @Override
    public void prepareForSaving() {
        super.prepareForSaving();
        metadata.add(gtaskMetadata);
    }
}
