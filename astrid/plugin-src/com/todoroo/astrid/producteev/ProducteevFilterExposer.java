/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev;

import java.util.Map.Entry;
import java.util.TreeMap;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.Pair;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.StoreObject;
import com.todoroo.astrid.producteev.sync.ProducteevDashboard;
import com.todoroo.astrid.producteev.sync.ProducteevDataService;
import com.todoroo.astrid.producteev.sync.ProducteevTask;

/**
 * Exposes filters based on Producteev Dashboards
 *
 * @author Arne Jans <arne.jans@gmail.com>
 *
 */
public class ProducteevFilterExposer extends BroadcastReceiver {

    /**
     * @param context
     */
    private Filter filterFromList(Context context, ProducteevDashboard dashboard) {
        String dashboardTitle = dashboard.getName();
        String title = dashboard.getName();
        ContentValues values = new ContentValues();
        values.put(Metadata.KEY.name, ProducteevTask.METADATA_KEY);
        values.put(ProducteevTask.DASHBOARD_ID.name, dashboard.getId());
        values.put(ProducteevTask.ID.name, 0);
        values.put(ProducteevTask.CREATOR_ID.name, 0);
        values.put(ProducteevTask.RESPONSIBLE_ID.name, 0);
        Filter filter = new Filter(dashboardTitle, title, new QueryTemplate().join(
                ProducteevDataService.METADATA_JOIN).where(Criterion.and(
                        MetadataCriteria.withKey(ProducteevTask.METADATA_KEY),
                        TaskCriteria.isActive(),
                        TaskCriteria.isVisible(),
                        ProducteevTask.DASHBOARD_ID.eq(dashboard.getId()))),
                values);

        return filter;
    }

    private Filter filterFromUser(Context context, String user, Pair<Long, String> ids) {
        String title = context.getString(R.string.producteev_FEx_responsible_title, user);
        ContentValues values = new ContentValues();
        values.put(Metadata.KEY.name, ProducteevTask.METADATA_KEY);
        values.put(ProducteevTask.DASHBOARD_ID.name, ids.getLeft());
        values.put(ProducteevTask.ID.name, 0);
        values.put(ProducteevTask.CREATOR_ID.name, 0);
        values.put(ProducteevTask.RESPONSIBLE_ID.name, ids.getRight());
        Filter filter = new Filter(user, title, new QueryTemplate().join(
                ProducteevDataService.METADATA_JOIN).where(Criterion.and(
                        MetadataCriteria.withKey(ProducteevTask.METADATA_KEY),
                        TaskCriteria.isActive(),
                        TaskCriteria.isVisible(),
                        ProducteevTask.RESPONSIBLE_ID.eq(ids.getRight()))),
                        values);

        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // if we aren't logged in, don't expose features
        if(!ProducteevUtilities.INSTANCE.isLoggedIn())
            return;

        StoreObject[] dashboards = ProducteevDataService.getInstance().getDashboards();

        // If user does not have any dashboards, don't show this section at all
        if(dashboards.length == 0)
            return;

        FilterListHeader producteevHeader = new FilterListHeader(context.getString(R.string.producteev_FEx_header));

        // load dashboards
        Filter[] dashboardFilters = new Filter[dashboards.length];
        for(int i = 0; i < dashboards.length; i++)
            dashboardFilters[i] = filterFromList(context, new ProducteevDashboard(dashboards[i]));
        FilterCategory producteevDashboards = new FilterCategory(context.getString(R.string.producteev_FEx_dashboard),
                dashboardFilters);

        // load responsible people
        TreeMap<String, Pair<Long, String>> people = loadResponsiblePeople(dashboards);
        Filter[] peopleFilters = new Filter[people.size()];
        int index = 0;
        for(Entry<String, Pair<Long, String>> person : people.entrySet())
            peopleFilters[index++] = filterFromUser(context, person.getKey(), person.getValue());
        FilterCategory producteevUsers = new FilterCategory(context.getString(R.string.producteev_FEx_responsible),
                peopleFilters);

        // transmit filter list
        FilterListItem[] list = new FilterListItem[3];
        list[0] = producteevHeader;
        list[1] = producteevDashboards;
        list[2] = producteevUsers;
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, ProducteevUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    /**
     * @param dashboards
     * @return people in a map of name => pair(dashboard id, user id)
     */
    @SuppressWarnings("nls")
    private TreeMap<String, Pair<Long, String>> loadResponsiblePeople(
                StoreObject[] dashboards) {
        TreeMap<String, Pair<Long, String>> results = new TreeMap<String, Pair<Long, String>>();
        for(StoreObject dashboard : dashboards) {
            String users = dashboard.getValue(ProducteevDashboard.USERS);
            String[] entries = users.split(";");
            for(String entry : entries) {
                String[] data = entry.split(",");
                if(data.length != 2)
                    continue;
                results.put(data[1], new Pair<Long, String>(
                        dashboard.getValue(ProducteevDashboard.REMOTE_ID), data[0])); // name, id
            }
        }

        return results;
    }

}
