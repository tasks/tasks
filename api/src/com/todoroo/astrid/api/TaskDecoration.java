/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.RemoteViews;
import android.widget.RemoteViews.RemoteView;

/**
 * Represents a line of text displayed in the Task List
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TaskDecoration implements Parcelable {

    /**
     * Place decoration between completion box and task title
     */
    public static final int POSITION_LEFT = 0;

    /**
     * Place decoration between task title and importance bar
     */
    public static final int POSITION_RIGHT = 1;

    /**
     * {@link RemoteView} decoration
     */
    public RemoteViews decoration = null;

    /**
     * Decoration position
     */
    public int position = POSITION_LEFT;

    /**
     * Decorated task background color. 0 is default
     */
    public int color = 0;

    /**
     * Creates a TaskDetail object
     * @param text
     *            text to display
     * @param color
     *            color to use for text. Use <code>0</code> for default color
     */
    public TaskDecoration(RemoteViews decoration, int position, int color) {
        this.decoration = decoration;
        this.position = position;
        this.color = color;
    }

    // --- parcelable helpers

    /**
     * {@inheritDoc}
     */
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(decoration, 0);
        dest.writeInt(position);
        dest.writeInt(color);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<TaskDecoration> CREATOR = new Parcelable.Creator<TaskDecoration>() {
        /**
         * {@inheritDoc}
         */
        public TaskDecoration createFromParcel(Parcel source) {
            return new TaskDecoration((RemoteViews)source.readParcelable(
                    RemoteViews.class.getClassLoader()),
                    source.readInt(), source.readInt());
        }

        /**
         * {@inheritDoc}
         */
        public TaskDecoration[] newArray(int size) {
            return new TaskDecoration[size];
        };
    };

}
