/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an item displayed by Astrid's FilterListActivity
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
abstract public class FilterListItem implements Parcelable {

    /**
     * Title of this item displayed on the Filters page
     */
    public String listingTitle = null;

    /**
     * Bitmap for icon used on listing page. <code>null</code> => no icon
     */
    public Bitmap listingIcon = null;

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

    // --- parcelable helpers

    /**
     * {@inheritDoc}
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(listingTitle);
        dest.writeParcelable(listingIcon, 0);

        // write array lengths before arrays
        dest.writeInt(contextMenuLabels.length);
        dest.writeStringArray(contextMenuLabels);
        dest.writeInt(contextMenuIntents.length);
        dest.writeTypedArray(contextMenuIntents, 0);
    }

    /**
     * Utility method to read FilterListItem properties from a parcel.
     *
     * @param source
     */
    public void readFromParcel(Parcel source) {
        listingTitle = source.readString();
        listingIcon = source.readParcelable(Bitmap.class.getClassLoader());

        int length = source.readInt();
        contextMenuLabels = new String[length];
        source.readStringArray(contextMenuLabels);
        length = source.readInt();
        contextMenuIntents = new Intent[length];
        source.readTypedArray(contextMenuIntents, Intent.CREATOR);
    }
}
