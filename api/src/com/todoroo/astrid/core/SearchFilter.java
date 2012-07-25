/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.astrid.api.FilterListItem;

/**
 * Special filter that triggers the search functionality when accessed.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SearchFilter extends FilterListItem {

    /**
     * Constructor for creating a new SearchFilter
     *
     * @param listingTitle
     *            Title of this item as displayed on the lists page, e.g. Inbox
     */
    public SearchFilter(String listingTitle) {
        this.listingTitle = listingTitle;
    }

    /**
     * Constructor for creating a new SearchFilter
     */
    protected SearchFilter() {
        //
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
            SearchFilter item = new SearchFilter();
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
