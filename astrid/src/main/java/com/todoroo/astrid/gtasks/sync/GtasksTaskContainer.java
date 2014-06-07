/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks.sync;

import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.sync.SyncContainer;

import java.util.ArrayList;

public class GtasksTaskContainer extends SyncContainer {

    public Metadata gtaskMetadata;

    public GtasksTaskContainer(Task task, ArrayList<Metadata> metadata, Metadata gtaskMetadata) {
        this.task = task;
        this.metadata = metadata;
        this.gtaskMetadata = gtaskMetadata;
    }

    @Override
    public void prepareForSaving() {
        super.prepareForSaving();
        metadata.add(gtaskMetadata);
    }
}
