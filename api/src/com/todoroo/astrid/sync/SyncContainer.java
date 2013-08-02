/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.sync;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;

import java.util.ArrayList;

/**
 * Container class for transmitting tasks and including local and remote
 * metadata. Synchronization Providers can subclass this class if desired.
 *
 * @author Tim Su <tim@todoroo.com>
 * @see SyncProvider
 */
public class SyncContainer {
    public Task task;
    public ArrayList<Metadata> metadata;

    /**
     * Check if the metadata contains anything with the given key
     *
     * @param key
     * @return first match. or null
     */
    public Metadata findMetadata(String key) {
        for (Metadata item : metadata) {
            if (AndroidUtilities.equals(key, item.getValue(Metadata.KEY))) {
                return item;
            }
        }
        return null;
    }

    /**
     * Method called when sync container is about to be saved into the database.
     */
    public void prepareForSaving() {
        // override me necessary
    }
}