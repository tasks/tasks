/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.producteev;

import java.util.Set;
import java.util.TreeSet;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.producteev.sync.ProducteevDashboard;
import com.todoroo.astrid.producteev.sync.ProducteevDataService;
import com.todoroo.astrid.producteev.sync.ProducteevTask;
import com.todoroo.astrid.producteev.sync.ProducteevUser;

public class ProducteevCustomFilterCriteriaExposer extends BroadcastReceiver {
    private static final String IDENTIFIER_PRODUCTEEV_WORKSPACE = "producteev_workspace"; //$NON-NLS-1$
    private static final String IDENTIFIER_PRODUCTEEV_ASSIGNEE = "producteev_assignee"; //$NON-NLS-1$

    @SuppressWarnings("nls")
    @Override
    public void onReceive(Context context, Intent intent) {
        // if we aren't logged in, don't expose features
        if(!ProducteevUtilities.INSTANCE.isLoggedIn())
            return;

        Resources r = context.getResources();

        StoreObject[] objects = ProducteevDataService.getInstance().getDashboards();
        ProducteevDashboard[] dashboards = new ProducteevDashboard[objects.length];
        for (int i = 0; i < objects.length; i++) {
            dashboards[i] = new ProducteevDashboard(objects[i]);
        }

        CustomFilterCriterion[] ret = new CustomFilterCriterion[2];
        int j = 0;

        {
            String[] workspaceNames = new String[objects.length];
            String[] workspaceIds = new String[objects.length];
            for (int i = 0; i < dashboards.length; i++) {
                workspaceNames[i] = dashboards[i].getName();
                workspaceIds[i] = String.valueOf(dashboards[i].getId());
            }
            ContentValues values = new ContentValues();
            values.put(Metadata.KEY.name, ProducteevTask.METADATA_KEY);
            values.put(ProducteevTask.DASHBOARD_ID.name, "?");
            CustomFilterCriterion criterion = new MultipleSelectCriterion(
                    IDENTIFIER_PRODUCTEEV_WORKSPACE,
                    context.getString(R.string.CFC_producteev_in_workspace_text),
                    // Todo: abstract these metadata queries
                    Query.select(Metadata.TASK).from(Metadata.TABLE).join(Join.inner(
                            Task.TABLE, Metadata.TASK.eq(Task.ID))).where(Criterion.and(
                            TaskDao.TaskCriteria.activeAndVisible(),
                            MetadataDao.MetadataCriteria.withKey(ProducteevTask.METADATA_KEY),
                            ProducteevTask.DASHBOARD_ID.eq("?"))).toString(),
                    values,
                    workspaceNames,
                    workspaceIds,
                    ((BitmapDrawable)r.getDrawable(R.drawable.silk_folder)).getBitmap(),
                    context.getString(R.string.CFC_producteev_in_workspace_name));
            ret[j++] = criterion;
        }

        {
            Set<ProducteevUser> users = new TreeSet<ProducteevUser>();
            for (ProducteevDashboard dashboard : dashboards) {
                users.addAll(dashboard.getUsers());
            }
            int numUsers = users.size();
            String[] userNames = new String[numUsers];
            String[] userIds = new String[numUsers];
            int i = 0;
            for (ProducteevUser user : users) {
                userNames[i] = user.toString();
                userIds[i] = String.valueOf(user.getId());
                i++;
            }
            ContentValues values = new ContentValues(2);
            values.put(Metadata.KEY.name, ProducteevTask.METADATA_KEY);
            values.put(ProducteevTask.RESPONSIBLE_ID.name, "?");
            CustomFilterCriterion criterion = new MultipleSelectCriterion(
                    IDENTIFIER_PRODUCTEEV_ASSIGNEE,
                    context.getString(R.string.CFC_producteev_assigned_to_text),
                    // Todo: abstract these metadata queries, and unify this code with the CustomFilterExposers.
                    Query.select(Metadata.TASK).from(Metadata.TABLE).join(Join.inner(
                            Task.TABLE, Metadata.TASK.eq(Task.ID))).where(Criterion.and(
                            TaskDao.TaskCriteria.activeAndVisible(),
                            MetadataDao.MetadataCriteria.withKey(ProducteevTask.METADATA_KEY),
                            ProducteevTask.RESPONSIBLE_ID.eq("?"))).toString(),
                    values,
                    userNames,
                    userIds,
                    ((BitmapDrawable)r.getDrawable(R.drawable.silk_user_gray)).getBitmap(),
                    context.getString(R.string.CFC_producteev_assigned_to_name));
            ret[j++] = criterion;
        }

        // transmit filter list
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_CUSTOM_FILTER_CRITERIA);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, ProducteevUtilities.IDENTIFIER);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, ret);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }
}
