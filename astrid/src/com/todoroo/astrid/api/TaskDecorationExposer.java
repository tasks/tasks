/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import com.todoroo.astrid.data.Task;

/**
 * API for exposing decorations displayed in the task list.
 *
 * Due to the limitations of the Android platform, this is currently
 * internal-use only, though if it can be done well, I would be open to creating
 * an external API.
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
