/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import com.todoroo.astrid.data.Task;


/**
 * API for exposing context menu items associated with a task.
 *
 * Due to the limitations of the Android platform, this is currently
 * internal-use only, though if it can be done well, I would be open to creating
 * an external API. Internal API for exposing task decorations
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public interface TaskContextActionExposer {

    public class TaskContextAction{
        public int labelResource;
        public Runnable action;

        public TaskContextAction(int labelResource, Runnable action) {
            super();
            this.labelResource = labelResource;
            this.action = action;
        }
    }

    /**
     * Expose context menu item label, or null if item should not be shown
     * @param task
     *
     * @return null if no item should be displayed, or string or id
     */
    public Object getLabel(Task task);

    /**
     * Call context menu action
     * @param task
     */
    public void invoke(Task task);

}
