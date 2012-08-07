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
 * Represents an intent that can be called to perform synchronization
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class SyncAction implements Parcelable {

    /**
     * Label
     */
    public String label = null;

    /**
     * Intent to call when invoking this operation
     */
    public PendingIntent intent = null;

    /**
     * Create an EditOperation object
     *
     * @param label
     *            label to display
     * @param intent
     *            intent to invoke
     */
    public SyncAction(String label, PendingIntent intent) {
        super();
        this.label = label;
        this.intent = intent;
    }

    /**
     * Returns the label of this action
     */
    @Override
    public String toString() {
        return label;
    }

    @Override
    public int hashCode() {
        return label.hashCode() ^ intent.getTargetPackage().hashCode();
    }

    /**
     * We consider two sync actions equal if target package is identical
     * and the labels are the same. This prevents duplicate pendingIntents
     * from creating multiple SyncAction objects.
     */
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof SyncAction))
            return false;
        SyncAction other = (SyncAction) o;
        return label.equals(other.label) && intent.getTargetPackage().equals(other.intent.getTargetPackage());
    }

    // --- parcelable helpers

    /**
     * {@inheritDoc}
     */
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label);
        dest.writeParcelable(intent, 0);
    }

    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<SyncAction> CREATOR = new Parcelable.Creator<SyncAction>() {
        /**
         * {@inheritDoc}
         */
        public SyncAction createFromParcel(Parcel source) {
            return new SyncAction(source.readString(), (PendingIntent)source.readParcelable(
                    PendingIntent.class.getClassLoader()));
        }

        /**
         * {@inheritDoc}
         */
        public SyncAction[] newArray(int size) {
            return new SyncAction[size];
        };
    };

}
