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
     *            task id
     * @param extended
     *            whether this request is for extended details (which are
     *            displayed when user presses a task), or standard (which are
     *            always displayed)
     * @return null if no details, or task details
     */
    public String getTaskDetails(Context context, long id, boolean extended);

    public String getPluginIdentifier();

}
