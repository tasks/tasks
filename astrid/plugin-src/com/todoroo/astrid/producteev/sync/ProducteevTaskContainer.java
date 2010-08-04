package com.todoroo.astrid.producteev.sync;

import java.util.ArrayList;
import java.util.Iterator;

import com.todoroo.astrid.api.TaskContainer;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.producteev.ProducteevTask;

/**
 * RTM Task Container
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ProducteevTaskContainer extends TaskContainer {
    public long id;
    public long dashboard;

    public ProducteevTaskContainer(Task task, ArrayList<Metadata> metadata, long id, long dashboard) {
        this.task = task;
        this.metadata = metadata;
        this.id = id;
        this.dashboard = dashboard;
    }

//    public ProducteevTaskContainer(Task task, ArrayList<Metadata> metadata,
//            RtmTaskSeries rtmTaskSeries) {
//        this(task, metadata, );
//    }

    public ProducteevTaskContainer(Task task, ArrayList<Metadata> metadata) {
        this(task, metadata, 0, 0);
        for(Iterator<Metadata> iterator = metadata.iterator(); iterator.hasNext(); ) {
            Metadata item = iterator.next();
            if(ProducteevTask.METADATA_KEY.equals(item.getValue(Metadata.KEY))) {
                if(item.containsNonNullValue(ProducteevTask.ID))
                    id = item.getValue(ProducteevTask.ID);
                if(item.containsNonNullValue(ProducteevTask.DASHBOARD_ID))
                    dashboard = item.getValue(ProducteevTask.DASHBOARD_ID);
                iterator.remove();
                break;
            }
        }
    }


}