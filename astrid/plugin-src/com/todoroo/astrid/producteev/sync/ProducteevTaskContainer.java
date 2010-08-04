package com.todoroo.astrid.producteev.sync;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;

import com.todoroo.astrid.api.TaskContainer;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;

/**
 * RTM Task Container
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ProducteevTaskContainer extends TaskContainer {

    public Metadata pdvTask;

    public ProducteevTaskContainer(Task task, ArrayList<Metadata> metadata, Metadata remote) {
        this.task = task;
        this.metadata = metadata;
        this.pdvTask = remote;
        if(this.pdvTask == null)
            this.pdvTask = new Metadata();
    }

    @SuppressWarnings("nls")
    public ProducteevTaskContainer(Task task, ArrayList<Metadata> metadata, JSONObject remoteTask) {
        this(task, metadata, new Metadata());
        pdvTask.setValue(ProducteevTask.ID, remoteTask.optLong("id_task"));
        pdvTask.setValue(ProducteevTask.DASHBOARD_ID, remoteTask.optLong("id_dashboard"));
    }

    public ProducteevTaskContainer(Task task, ArrayList<Metadata> metadata) {
        this.task = task;
        this.metadata = metadata;

        for(Iterator<Metadata> iterator = metadata.iterator(); iterator.hasNext(); ) {
            Metadata item = iterator.next();
            if(ProducteevTask.METADATA_KEY.equals(item.getValue(Metadata.KEY))) {
                pdvTask = item;
                iterator.remove();
                break;
            }
        }
        if(this.pdvTask == null)
            this.pdvTask = new Metadata();
    }


}