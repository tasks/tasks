package com.todoroo.astrid.api;

import java.util.ArrayList;

import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;

/**
 * Container class for tasks. Synchronization Providers can subclass
 * this class if desired.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TaskContainer {
    public Task task;
    public ArrayList<Metadata> metadata;
}