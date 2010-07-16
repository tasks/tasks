package com.todoroo.astrid.api;

import android.content.Context;

/**
 * Internal API for Task Details
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public interface DetailExposer {

    /**
     * @param id
     * @return null if no details, or task details
     */
    public TaskDetail getTaskDetails(Context context, long id);

}
