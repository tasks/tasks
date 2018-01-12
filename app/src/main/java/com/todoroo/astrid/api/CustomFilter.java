package com.todoroo.astrid.api;

import android.os.Parcel;
import android.os.Parcelable;

import com.todoroo.andlib.utility.AndroidUtilities;
import org.tasks.data.StoreObject;

import java.util.Map;

public class CustomFilter extends Filter {
    private long id;

    private CustomFilter() {

    }

    public CustomFilter(String listingTitle, String sql, Map<String, Object> values, long id) {
        super(listingTitle, sql, values);
        this.id = id;
    }

    public StoreObject toStoreObject() {
        StoreObject storeObject = new StoreObject();
        storeObject.setId(id);
        storeObject.setItem(listingTitle);
        storeObject.setValue(sqlQuery);
        if (valuesForNewTasks != null && valuesForNewTasks.size() > 0) {
            storeObject.setValue2(AndroidUtilities.mapToSerializedString(valuesForNewTasks));
        }
        return storeObject;
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
