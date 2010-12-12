package com.todoroo.astrid.producteev.sync;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;

import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.sync.SyncContainer;

/**
 * RTM Task Container
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ProducteevTaskContainer extends SyncContainer {

    public Metadata pdvTask;
//    private Resources res;

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
//        res = ContextManager.getContext().getResources();
        pdvTask.setValue(Metadata.KEY, ProducteevTask.METADATA_KEY);
        pdvTask.setValue(ProducteevTask.ID, remoteTask.optLong("id_task"));
        pdvTask.setValue(ProducteevTask.DASHBOARD_ID, remoteTask.optLong("id_dashboard"));
        pdvTask.setValue(ProducteevTask.RESPONSIBLE_ID, remoteTask.optLong("id_responsible"));
        pdvTask.setValue(ProducteevTask.CREATOR_ID, remoteTask.optLong("id_creator"));
        String repeatingValue = remoteTask.optString("repeating_value");
        String repeatingInterval = remoteTask.optString("repeating_interval");
        if (!"0".equals(repeatingValue) && repeatingValue.length() > 0 &&
                repeatingInterval != null && repeatingInterval.length() > 0) {
            pdvTask.setValue(ProducteevTask.REPEATING_SETTING, repeatingValue+","+repeatingInterval);
//                    "PDV "+("1".equals(repeatingValue) ?
//                            res.getString(R.string.repeat_every).replaceAll(" %d", "") :
//                            res.getString(R.string.repeat_every, Integer.parseInt(repeatingValue)))+" "+getLocalizedRepeatInterval(repeatingInterval));
        }
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

//    private String getLocalizedRepeatInterval(String pdvRepeatInterval) {
//        String localizedRepeatInterval = "";
//        if (pdvRepeatInterval.startsWith("day"))
//            localizedRepeatInterval = res.getStringArray(R.array.repeat_interval)[0];
//        else if (pdvRepeatInterval.equals("week") || pdvRepeatInterval.equals("weeks"))
//            localizedRepeatInterval = res.getStringArray(R.array.repeat_interval)[1];
//        else if (pdvRepeatInterval.startsWith("month"))
//            localizedRepeatInterval = res.getStringArray(R.array.repeat_interval)[2];
//        else
//            localizedRepeatInterval = pdvRepeatInterval; // TODO: localize year and weekday in strings-producteev.xml
//        return localizedRepeatInterval;
//    }
}