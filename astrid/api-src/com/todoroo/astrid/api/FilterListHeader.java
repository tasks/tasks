/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Section Header for Filter List
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterListHeader extends FilterListItem {

    /**
     * Plug-in Identifier
     */
    public final String plugin;

    /**
     * Constructor for creating a new FilterListHeader
     *
     * @param plugin
     *            {@link Plugin} identifier that encompasses object
     * @param listingTitle
     * @param listingIconResource
     * @param priority
     */
    public FilterListHeader(String plugin, String listingTitle) {
        this.plugin = plugin;
        this.listingTitle = listingTitle;
    }

    /**
     * Constructor for creating a new FilterListHeader
     *
     * @param plugin
     *            {@link Plugin} identifier that encompasses object
     */
    protected FilterListHeader(String plugin) {
        this.plugin = plugin;
    }

    // --- parcelable

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(plugin);
        super.writeToParcel(dest, flags);
    }

    public static final Parcelable.Creator<FilterListHeader> CREATOR = new Parcelable.Creator<FilterListHeader>() {

        public FilterListHeader createFromParcel(Parcel source) {
            FilterListHeader item = new FilterListHeader(source.readString());
            item.readFromParcel(source);
            return item;
        }

        public FilterListHeader[] newArray(int size) {
            return new FilterListHeader[size];
        }

    };
}
