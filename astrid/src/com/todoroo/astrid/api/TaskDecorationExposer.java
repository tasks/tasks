package com.todoroo.astrid.api;

import com.todoroo.astrid.data.Task;

/**
 * Internal API for exposing task decorations
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public interface TaskDecorationExposer {

    /**
     * Expose task decorations for the given task
     * @param task
     *
     * @return null if no decorations, or decoration
     */
    public TaskDecoration expose(Task task);

    public String getAddon();

}
