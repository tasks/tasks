/**
 * Copyright (c) 2012 Todoroo Inc
 *
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
     * Constructor for creating a new FilterListHeader
     */
    public FilterListHeader(String listingTitle) {
        this.listingTitle = listingTitle;
    }

    /**
     * Constructor for creating a new FilterListHeader
     */
    protected FilterListHeader() {
        //
    }

    // --- parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<FilterListHeader> CREATOR = new Parcelable.Creator<FilterListHeader>() {

        @Override
        public FilterListHeader createFromParcel(Parcel source) {
            FilterListHeader item = new FilterListHeader();
            item.readFromParcel(source);
            return item;
        }

        @Override
        public FilterListHeader[] newArray(int size) {
            return new FilterListHeader[size];
        }

    };
}
