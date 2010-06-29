/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a line of text displayed in the Task List
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TaskDetail implements Parcelable {

    /**
     * Plug-in Identifier
     */
    public final String plugin;

    /**
     * Text of detail
     */
    public String text = null;

    /**
     * Color to use for text. 0 is default
     */
    public int color = 0;

    /**
     * Creates a TaskDetail object
     *
     * @param plugin
     *            {@link Plugin} identifier that encompasses object
     * @param text
     *            text to display
     * @param color
     *            color to use for text. Use <code>0</code> for default color
     */
    public TaskDetail(String plugin, String text, int color) {
        this.plugin = plugin;
        this.text = text;
        this.color = color;
    }

    /**
     * Convenience constructor to make a TaskDetail with default color
     *
     * @param text
     *            text to display
     */
    public TaskDetail(String plugin, String text) {
        this(plugin, text, 0);
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
        dest.writeString(plugin);
        dest.writeString(text);
        dest.writeInt(color);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<TaskDetail> CREATOR = new Parcelable.Creator<TaskDetail>() {
        /**
         * {@inheritDoc}
         */
        public TaskDetail createFromParcel(Parcel source) {
            return new TaskDetail(source.readString(), source.readString(), source.readInt());
        }

        /**
         * {@inheritDoc}
         */
        public TaskDetail[] newArray(int size) {
            return new TaskDetail[size];
        };
    };

}
