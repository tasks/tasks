/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.activity.FilterListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.dao.MetadataDao.MetadataCriteria;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.TagService;

/**
 * Exposes Astrid's built in filters to the {@link FilterListActivity}
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class CoreFilterExposer extends BroadcastReceiver implements AstridFilterExposer {

    @Override
    public void onReceive(Context context, Intent intent) {
        Resources r = context.getResources();
        ContextManager.setContext(context);

        FilterListItem[] list = prepareFilters(r);
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private FilterListItem[] prepareFilters(Resources r) {
        // core filters
        Filter inbox = buildInboxFilter(r);


        // transmit filter list
        FilterListItem[] list = new FilterListItem[1];
        list[0] = inbox;
        return list;
    }

    /**
     * Build inbox filter
     * @return
     */
    public static Filter buildInboxFilter(Resources r) {
        Filter inbox = new Filter(r.getString(R.string.BFE_Active), r.getString(R.string.BFE_Active),
                new QueryTemplate().where(
                        Criterion.and(TaskCriteria.activeVisibleMine(),
                                Criterion.not(Task.ID.in(Query.select(Metadata.TASK).from(Metadata.TABLE).where(
                                        Criterion.and(MetadataCriteria.withKey(TagService.KEY),
                                                TagService.TAG.like("x_%", "x"))))))), //$NON-NLS-1$ //$NON-NLS-2$
                null);
        inbox.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.filter_inbox)).getBitmap();
        return inbox;
    }

    @Override
    public FilterListItem[] getFilters() {
        if (ContextManager.getContext() == null || ContextManager.getContext().getResources() == null)
            return null;

        Resources r = ContextManager.getContext().getResources();
        return prepareFilters(r);
    }

}
