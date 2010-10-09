package com.todoroo.astrid.api;

import android.content.ComponentName;
import android.content.ContentValues;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.andlib.sql.QueryTemplate;

public class FilterWithCustomIntent extends Filter {

    public ComponentName customTaskList = null;
    public Bundle customExtras = null;

    protected FilterWithCustomIntent() {
        super();
    }

    public FilterWithCustomIntent(String listingTitle, String title,
            QueryTemplate sqlQuery, ContentValues valuesForNewTasks) {
        super(listingTitle, title, sqlQuery, valuesForNewTasks);
    }

    public FilterWithCustomIntent(String listingTitle, String title,
            String sqlQuery, ContentValues valuesForNewTasks) {
        super(listingTitle, title, sqlQuery, valuesForNewTasks);
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
        dest.writeParcelable(customTaskList, 0);
        dest.writeParcelable(customExtras, 0);
    }

    @Override
    public void readFromParcel(Parcel source) {
        super.readFromParcel(source);
        customTaskList = source.readParcelable(ComponentName.class.getClassLoader());
        customExtras = source.readParcelable(Bundle.class.getClassLoader());
    }

    /**
     * Parcelable Creator Object
     */
    @SuppressWarnings("hiding")
    public static final Parcelable.Creator<FilterWithCustomIntent> CREATOR = new Parcelable.Creator<FilterWithCustomIntent>() {

        /**
         * {@inheritDoc}
         */
        public FilterWithCustomIntent createFromParcel(Parcel source) {
            FilterWithCustomIntent item = new FilterWithCustomIntent();
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        public FilterWithCustomIntent[] newArray(int size) {
            return new FilterWithCustomIntent[size];
        }

    };

}
