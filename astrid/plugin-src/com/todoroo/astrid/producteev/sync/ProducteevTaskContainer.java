package com.todoroo.astrid.producteev.sync;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;

import com.todoroo.astrid.common.TaskContainer;
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

    public ProducteevTaskContainer(Task task, ArrayList<Metadata> metadata, Metadata pdvTask) {
        this.task = task;
        this.metadata = metadata;
        this.pdvTask = pdvTask;
        if(this.pdvTask == null) {
            this.pdvTask = ProducteevTask.newMetadata();
        }
    }

    @SuppressWarnings("nls")
    public ProducteevTaskContainer(Task task, ArrayList<Metadata> metadata, JSONObject remoteTask) {
        this(task, metadata, new Metadata());
        pdvTask.setValue(Metadata.KEY, ProducteevTask.METADATA_KEY);
        pdvTask.setValue(ProducteevTask.ID, remoteTask.optLong("id_task"));
        pdvTask.setValue(ProducteevTask.DASHBOARD_ID, remoteTask.optLong("id_dashboard"));
        pdvTask.setValue(ProducteevTask.RESPONSIBLE_ID, remoteTask.optLong("id_responsible"));
        pdvTask.setValue(ProducteevTask.CREATOR_ID, remoteTask.optLong("id_creator"));
    }

    public ProducteevTaskContainer(Task task, ArrayList<Metadata> metadata) {
        this.task = task;
        this.metadata = metadata;

        for(Iterator<Metadata> iterator = metadata.iterator(); iterator.hasNext(); ) {
            Metadata item = iterator.next();
            if(ProducteevTask.METADATA_KEY.equals(item.getValue(Metadata.KEY))) {
                pdvTask = item;
                iterator.remove();
                // don't break, could be multiple
            }
        }
        if(this.pdvTask == null) {
            this.pdvTask = ProducteevTask.newMetadata();
        }
    }


}