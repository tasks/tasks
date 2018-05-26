/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an item displayed by Astrid's FilterListActivity
 *
 * @author Tim Su <tim@todoroo.com>
 */
public abstract class FilterListItem implements Parcelable {

  /** Title of this item displayed on the Filters page */
  public String listingTitle = null;

  public int icon = 0;
  public int tint = -1;

  public abstract Type getItemType();

  @Override
  public int describeContents() {
    return 0;
  }

  /** {@inheritDoc} */
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(listingTitle);
    dest.writeInt(icon);
    dest.writeInt(tint);
    dest.writeStringArray(new String[0]); // old context menu labels
    dest.writeTypedArray(new Intent[0], 0); // old context menu intents
  }

  // --- parcelable helpers

  /** Utility method to read FilterListItem properties from a parcel. */
  protected void readFromParcel(Parcel source) {
    listingTitle = source.readString();
    icon = source.readInt();
    tint = source.readInt();
    source.createStringArray(); // old context menu labels
    source.createTypedArray(Intent.CREATOR); // old context menu intents
  }

  @Override
  public String toString() {
    return "FilterListItem{" + "listingTitle='" + listingTitle + '\'' + '}';
  }

  public enum Type {
    ITEM,
    SUBHEADER,
    SEPARATOR
  }
}
