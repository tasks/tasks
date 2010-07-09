package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Special filter that triggers the search functionality when accessed.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SearchFilter extends FilterListItem {

    /**
     * Plug-in Identifier
     */
    public final String plugin;

    /**
     * Constructor for creating a new SearchFilter
     *
     * @param plugin
     *            {@link Addon} identifier that encompasses object
     * @param listingTitle
     *            Title of this item as displayed on the lists page, e.g. Inbox
     */
    public SearchFilter(String plugin, String listingTitle) {
        this.plugin = plugin;
        this.listingTitle = listingTitle;
    }

    /**
     * Constructor for creating a new SearchFilter
     *
     * @param plugin
     *            {@link Addon} identifier that encompasses object
     */
    protected SearchFilter(String plugin) {
        this.plugin = plugin;
    }

    // --- parcelable

    /**
     * {@inheritDoc}
     */
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(plugin);
        super.writeToParcel(dest, flags);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<SearchFilter> CREATOR = new Parcelable.Creator<SearchFilter>() {

        /**
         * {@inheritDoc}
         */
        public SearchFilter createFromParcel(Parcel source) {
            SearchFilter item = new SearchFilter(source.readString());
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        public SearchFilter[] newArray(int size) {
            return new SearchFilter[size];
        }

    };

}
