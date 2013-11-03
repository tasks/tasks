/**
 *
 */
package com.todoroo.astrid.helper;

import android.support.v4.app.ListFragment;
import android.widget.ListView;

import com.todoroo.astrid.adapter.TaskAdapter.ViewHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

abstract public class TaskAdapterAddOnManager<TYPE> {

    private final ListFragment fragment;

    protected TaskAdapterAddOnManager(ListFragment fragment) {
        this.fragment = fragment;
    }

    private final Map<Long, LinkedHashMap<String, TYPE>> cache =
        Collections.synchronizedMap(new HashMap<Long, LinkedHashMap<String, TYPE>>(0));

    // --- interface

    /** updates the given view */
    abstract protected void draw(ViewHolder viewHolder, long taskId, Collection<TYPE> list);

    /** on receive an intent */
    public void addNew(long taskId, String addOn, TYPE item, ViewHolder thisViewHolder) {
        if(item == null) {
            return;
        }

        Collection<TYPE> cacheList = addIfNotExists(taskId, addOn, item);
        if(cacheList != null) {
            if(thisViewHolder != null) {
                draw(thisViewHolder, taskId, cacheList);
            } else {
                ListView listView = fragment.getListView();
                // update view if it is visible
                int length = listView.getChildCount();
                for(int i = 0; i < length; i++) {
                    ViewHolder viewHolder = (ViewHolder) listView.getChildAt(i).getTag();
                    if(viewHolder == null || viewHolder.task.getId() != taskId) {
                        continue;
                    }
                    draw(viewHolder, taskId, cacheList);
                    break;
                }
            }
        }
    }

    /**
     * Clears the cache
     */
    public void clearCache() {
        cache.clear();
    }

    // --- internal goodies

    /**
     * Adds an item to the cache if it doesn't exist
     * @return iterator if item was added, null if it already existed
     */
    protected synchronized Collection<TYPE> addIfNotExists(long taskId, String addOn,
            TYPE item) {
        LinkedHashMap<String, TYPE> list = cache.get(taskId);
        if(list == null) {
            return null;
        }
        if(list.containsValue(item)) {
            return null;
        }
        list.put(addOn, item);
        return get(taskId);
    }

    /**
     * Gets an item at the given index
     */
    protected Collection<TYPE> get(long taskId) {
        if(cache.get(taskId) == null) {
            return null;
        }
        return cache.get(taskId).values();
    }

}
