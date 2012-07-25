/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an add-onn for Astrid. Users can enable or disable add-ons,
 * which affect all other extension points that share the same identifier.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class Addon implements Parcelable {

    /**
     * Add-on Identifier
     */
    public String addon = null;

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
     * @param addon
     * @param title
     * @param author
     * @param description
     */
    public Addon(String addon, String title, String author, String description) {
        this.addon = addon;
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
        dest.writeString(addon);
        dest.writeString(title);
        dest.writeString(author);
        dest.writeString(description);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<Addon> CREATOR = new Parcelable.Creator<Addon>() {
        /**
         * {@inheritDoc}
         */
        public Addon createFromParcel(Parcel source) {
            return new Addon(source.readString(), source.readString(),
                    source.readString(), source.readString());
        }

        /**
         * {@inheritDoc}
         */
        public Addon[] newArray(int size) {
            return new Addon[size];
        };
    };

}
