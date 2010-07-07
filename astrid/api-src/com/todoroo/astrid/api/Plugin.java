/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a plug-in for Astrid. Users can enable or disable plug-ins,
 * which affect all other extension points that share the same identifier.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class Plugin implements Parcelable {

    /**
     * Plug-in Identifier
     */
    public String plugin = null;

    /**
     * Plug-in Title
     */
    public String title = null;

    /**
     * Plug-in Author
     */
    public String author = null;

    /**
     * Plug-in Description
     */
    public String description = null;

    /**
     * Convenience constructor to generate a plug-in object
     *
     * @param plugin
     * @param title
     * @param author
     * @param description
     */
    public Plugin(String plugin, String title, String author, String description) {
        this.plugin = plugin;
        this.title = title;
        this.author = author;
        this.description = description;
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
        dest.writeString(title);
        dest.writeString(author);
        dest.writeString(description);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<Plugin> CREATOR = new Parcelable.Creator<Plugin>() {
        /**
         * {@inheritDoc}
         */
        public Plugin createFromParcel(Parcel source) {
            return new Plugin(source.readString(), source.readString(),
                    source.readString(), source.readString());
        }

        /**
         * {@inheritDoc}
         */
        public Plugin[] newArray(int size) {
            return new Plugin[size];
        };
    };

}
