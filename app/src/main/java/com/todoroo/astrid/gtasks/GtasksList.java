/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import com.google.common.base.Strings;
import org.tasks.data.StoreObject;

/**
 * {@link StoreObject} entries for a GTasks List
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class GtasksList {

    private StoreObject storeObject;

    public GtasksList(final String remoteId) {
        this(newStoreObject());
        setLastSync(0L);
        setRemoteId(remoteId);
    }

    private static StoreObject newStoreObject() {
        StoreObject storeObject = new StoreObject();
        storeObject.setType(GtasksList.TYPE);
        return storeObject;
    }

    public GtasksList(StoreObject storeObject) {
        if (!storeObject.getType().equals(TYPE)) {
            throw new RuntimeException("Type is not " + TYPE);
        }
        this.storeObject = storeObject;
    }

    /**
     * type
     */
    public static final String TYPE = "gtasks-list"; //$NON-NLS-1$

    public Long getId() {
        return storeObject.getId();
    }

    public String getRemoteId() {
        return storeObject.getItem();
    }

    private void setRemoteId(String remoteId) {
        storeObject.setItem(remoteId);
    }

    public String getName() {
        return storeObject.getValue();
    }

    public void setName(String name) {
        storeObject.setValue(name);
    }

    public void setOrder(int order) {
        storeObject.setValue2(Integer.toString(order));
    }

    public int getColor() {
        String color = storeObject.getValue4();
        return Strings.isNullOrEmpty(color) ? -1 : Integer.parseInt(storeObject.getValue4());
    }

    public void setColor(int color) {
        storeObject.setValue4(Integer.toString(color));
    }

    public long getLastSync() {
        String lastSync = storeObject.getValue3();
        return Strings.isNullOrEmpty(lastSync) ? 0 : Long.parseLong(lastSync);
    }

    public void setLastSync(long timestamp) {
        storeObject.setValue3(Long.toString(timestamp));
    }

    public StoreObject getStoreObject() {
        return storeObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof GtasksList)) return false;

        GtasksList that = (GtasksList) o;

        return storeObject != null ? storeObject.equals(that.storeObject) : that.storeObject == null;
    }

    @Override
    public int hashCode() {
        return storeObject != null ? storeObject.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "GtasksList{" +
                "storeObject=" + storeObject +
                '}';
    }
}
