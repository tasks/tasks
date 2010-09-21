/**
 *
 */
package com.todoroo.astrid.helper;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.widget.ListView;

import com.todoroo.astrid.adapter.TaskAdapter.ViewHolder;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.data.Task;

abstract public class TaskAdapterAddOnManager<TYPE> {

    private final ListActivity activity;

    /**
     * @param taskAdapter
     */
    protected TaskAdapterAddOnManager(ListActivity activity) {
        this.activity = activity;
    }

    private final Map<Long, HashMap<String, TYPE>> cache =
        Collections.synchronizedMap(new HashMap<Long, HashMap<String, TYPE>>(0));

    // --- interface

    /**
     * Request add-ons for the given task
     * @return true if cache miss, false if cache hit
     */
    public boolean request(ViewHolder viewHolder) {
        long taskId = viewHolder.task.getId();

        Collection<TYPE> list = initialize(taskId);
        if(list != null) {
            draw(viewHolder, taskId, list);
            return false;
        }

        // request details
        draw(viewHolder, taskId, get(taskId));
        Intent broadcastIntent = createBroadcastIntent(viewHolder.task);
        if(broadcastIntent != null)
            activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        return true;
    }

    /** creates a broadcast intent for requesting */
    abstract protected Intent createBroadcastIntent(Task task);

    /** updates the given view */
    abstract protected void draw(ViewHolder viewHolder, long taskId, Collection<TYPE> list);

    /** resets the view as if there was nothing */
    abstract protected void reset(ViewHolder viewHolder, long taskId);

    /** on receive an intent */
    public void addNew(long taskId, String addOn, TYPE item) {
        if(item == null)
            return;

        Collection<TYPE> cacheList = addIfNotExists(taskId, addOn, item);
        if(cacheList != null) {
            ListView listView = activity.getListView();
            // update view if it is visible
            int length = listView.getChildCount();
            for(int i = 0; i < length; i++) {
                ViewHolder viewHolder = (ViewHolder) listView.getChildAt(i).getTag();
                if(viewHolder == null || viewHolder.task.getId() != taskId)
                    continue;
                draw(viewHolder, taskId, cacheList);
                break;
            }
        }
    }

    /**
     * Clears the cache
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Clears single item from cache
     */
    public void clearCache(long taskId) {
        cache.remove(taskId);
    }

    // --- internal goodies

    /**
     * Retrieves a list. If it doesn't exist, list is created, but
     * the method will return null
     * @param taskId
     * @return list if there was already one
     */
    protected synchronized Collection<TYPE> initialize(long taskId) {
        if(cache.containsKey(taskId) && cache.get(taskId) != null)
            return get(taskId);
        cache.put(taskId, new HashMap<String, TYPE>(0));
        return null;
    }

    /**
     * Adds an item to the cache if it doesn't exist
     * @param taskId
     * @param item
     * @return iterator if item was added, null if it already existed
     */
    protected synchronized Collection<TYPE> addIfNotExists(long taskId, String addOn,
            TYPE item) {
        HashMap<String, TYPE> list = cache.get(taskId);
        if(list == null)
            return null;
        if(list.containsKey(addOn) && list.get(addOn).equals(item))
            return null;
        list.put(addOn, item);
        return get(taskId);
    }

    /**
     * Gets an item at the given index
     * @param taskId
     * @return
     */
    protected Collection<TYPE> get(long taskId) {
        return cache.get(taskId).values();
    }

}