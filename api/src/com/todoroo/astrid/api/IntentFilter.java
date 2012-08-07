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
 * Special filter that launches a PendingIntent when accessed.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class IntentFilter extends FilterListItem implements Parcelable {

    /**
     * PendingIntent to trigger when pressed
     */
    public PendingIntent intent;

    /**
     * Constructor for creating a new IntentFilter
     *
     * @param listingTitle
     *            Title of this item as displayed on the lists page, e.g. Inbox
     * @param intent
     *            intent to load
     */
    public IntentFilter(String listingTitle, PendingIntent intent) {
        this.listingTitle = listingTitle;
        this.intent = intent;
    }

    /**
     * Constructor for creating a new IntentFilter used internally
     */
    protected IntentFilter(PendingIntent intent) {
        this.intent = intent;
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
        dest.writeParcelable(intent, 0);
        super.writeToParcel(dest, flags);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<IntentFilter> CREATOR = new Parcelable.Creator<IntentFilter>() {

        /**
         * {@inheritDoc}
         */
        public IntentFilter createFromParcel(Parcel source) {
            IntentFilter item = new IntentFilter((PendingIntent) source.readParcelable(
                    PendingIntent.class.getClassLoader()));
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        public IntentFilter[] newArray(int size) {
            return new IntentFilter[size];
        }

    };

}
