package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;

import static com.todoroo.andlib.utility.AndroidUtilities.mapToSerializedString;

public class CustomFilter extends Filter {
    private long id;

    private CustomFilter() {

    }

    public CustomFilter(String listingTitle, String sql, Map<String, Object> values, long id) {
        super(listingTitle, sql, values);
        this.id = id;
    }

    public org.tasks.data.Filter toStoreObject() {
        org.tasks.data.Filter filter = new org.tasks.data.Filter();
        filter.setId(id);
        filter.setTitle(listingTitle);
        filter.setSql(sqlQuery);
        if (valuesForNewTasks != null && valuesForNewTasks.size() > 0) {
            filter.setCriterion(mapToSerializedString(valuesForNewTasks));
        }
        return filter;
    }

    public long getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(id);
    }

    @Override
    protected void readFromParcel(Parcel source) {
        super.readFromParcel(source);
        id = source.readLong();
    }

    /**
     * Parcelable Creator Object
     */
    public static final Parcelable.Creator<CustomFilter> CREATOR = new Parcelable.Creator<CustomFilter>() {

        /**
         * {@inheritDoc}
         */
        @Override
        public CustomFilter createFromParcel(Parcel source) {
            CustomFilter item = new CustomFilter();
            item.readFromParcel(source);
            return item;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CustomFilter[] newArray(int size) {
            return new CustomFilter[size];
        }
    };
}
