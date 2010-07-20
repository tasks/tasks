package com.todoroo.astrid.api;

import java.util.ArrayList;

import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;

/**
 * Container class for transmitting tasks and including local and remote
 * metadata. Synchronization Providers can subclass this class if desired.
 *
 * @see {@link SynchronizationProvider}
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskContainer {
    public Task task;
    public ArrayList<Metadata> metadata;
}