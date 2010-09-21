package com.todoroo.astrid.gtasks.sync;

import java.util.ArrayList;
import java.util.Iterator;

import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksMetadata;
import com.todoroo.astrid.producteev.sync.ProducteevTask;
import com.todoroo.astrid.sync.SyncContainer;

/**
 * RTM Task Container
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksTaskContainer extends SyncContainer {

    public Metadata gtaskMetadata;

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
            if(ProducteevTask.METADATA_KEY.equals(item.getValue(Metadata.KEY))) {
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