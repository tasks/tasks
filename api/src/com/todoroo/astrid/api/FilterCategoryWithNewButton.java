/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A <code>FilterCategoryWithNewButton</code> has a button for new filter creation
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterCategoryWithNewButton extends FilterCategory {

    /**
     * Intent to launch
     */
    public PendingIntent intent;

    /**
     * Label for new button
     */
    public String label;

    /**
     * Constructor for creating a new FilterCategory
     * @param listingTitle
     *            Title of this item as displayed on the lists page, e.g. Inbox
     * @param children
     *            filters belonging to this category
     */
    public FilterCategoryWithNewButton(String listingTitle, Filter[] children) {
        this.listingTitle = listingTitle;
        this.children = children;
    }

    /**
     * Constructor for creating a new FilterCategory
     *
     * @param plugin
     *            {@link Addon} identifier that encompasses object
     */
    protected FilterCategoryWithNewButton() {
        //
    }

    // --- parcelable

    /**
     * {@inheritDoc}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(intent, 0);
        dest.writeString(label);
    }

    /**
     * Parcelable creator
     */
    @SuppressWarnings("hiding")
    public static final Parcelable.Creator<FilterCategoryWithNewButton> CREATOR = new Parcelable.Creator<FilterCategoryWithNewButton>() {

        /**
         * {@inheritDoc}
         */
        public FilterCategoryWithNewButton createFromParcel(Parcel source) {
            FilterCategoryWithNewButton item = new FilterCategoryWithNewButton();
            item.readFromParcel(source);

            Parcelable[] parcelableChildren = source.readParcelableArray(
                    FilterCategoryWithNewButton.class.getClassLoader());
            item.children = new Filter[parcelableChildren.length];
            for(int i = 0; i < item.children.length; i++) {
                if(parcelableChildren[i] instanceof FilterListItem)
                    item.children[i] = (Filter) parcelableChildren[i];
                else
                    item.children[i] = null;
            }

            item.intent = source.readParcelable(PendingIntent.class.getClassLoader());
            item.label = source.readString();

            return item;
        }

        /**
         * {@inheritDoc}
         */
        public FilterCategoryWithNewButton[] newArray(int size) {
            return new FilterCategoryWithNewButton[size];
        }

    };
}
