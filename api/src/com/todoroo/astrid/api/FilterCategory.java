/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A <code>FilterCategory</code> groups common {@link Filter}s and allows
 * a user to show/hide all of its children.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterCategory extends FilterListItem {

    /**
     * {@link Filter}s contained by this category
     */
    public Filter[] children;

    /**
     * Constructor for creating a new FilterCategory
     * @param listingTitle
     *            Title of this item as displayed on the lists page, e.g. Inbox
     * @param children
     *            filters belonging to this category
     */
    public FilterCategory(String listingTitle, Filter[] children) {
        this.listingTitle = listingTitle;
        this.children = children;
    }

    /**
     * Constructor for creating a new FilterCategory
     *
     * @param plugin
     *            {@link Addon} identifier that encompasses object
     */
    protected FilterCategory() {
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
        dest.writeParcelableArray(children, 0);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<FilterCategory> CREATOR = new Parcelable.Creator<FilterCategory>() {

        /**
         * {@inheritDoc}
         */
        public FilterCategory createFromParcel(Parcel source) {
            FilterCategory item = new FilterCategory();
            item.readFromParcel(source);

            Parcelable[] parcelableChildren = source.readParcelableArray(
                    FilterCategory.class.getClassLoader());
            item.children = new Filter[parcelableChildren.length];
            for(int i = 0; i < item.children.length; i++) {
                if(parcelableChildren[i] instanceof FilterListItem)
                    item.children[i] = (Filter) parcelableChildren[i];
                else
                    item.children[i] = null;
            }

            return item;
        }

        /**
         * {@inheritDoc}
         */
        public FilterCategory[] newArray(int size) {
            return new FilterCategory[size];
        }

    };
}
