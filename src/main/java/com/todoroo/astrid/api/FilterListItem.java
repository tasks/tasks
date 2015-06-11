/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an item displayed by Astrid's FilterListActivity
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class FilterListItem implements Parcelable {

    public enum Type {
        ITEM(0),
        SUBHEADER(1),
        SEPARATOR(2);

        private int viewType;

        Type(int viewType) {
            this.viewType = viewType;
        }

        public int getViewType() {
            return viewType;
        }
    }

    public abstract Type getItemType();

    /**
     * Title of this item displayed on the Filters page
     */
    public String listingTitle = null;

    public int icon = 0;

    /**
     * Context Menu labels. The context menu will be displayed when users
     * long-press on this filter list item.
     */
    public String contextMenuLabels[] = new String[0];

    /**
     * Context menu intents. This intent will be started when the corresponding
     * content menu label is invoked. This array must be the same size as
     * the contextMenuLabels array.
     */
    public Intent contextMenuIntents[] = new Intent[0];

    @Override
    public int describeContents() {
        return 0;
    }

    // --- parcelable helpers

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(listingTitle);
        dest.writeInt(icon);

        // write array lengths before arrays
        dest.writeStringArray(contextMenuLabels);
        dest.writeTypedArray(contextMenuIntents, 0);
    }

    /**
     * Utility method to read FilterListItem properties from a parcel.
     */
    public void readFromParcel(Parcel source) {
        listingTitle = source.readString();
        icon = source.readInt();

        contextMenuLabels = source.createStringArray();
        contextMenuIntents = source.createTypedArray(Intent.CREATOR);
    }
}
