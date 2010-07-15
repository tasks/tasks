/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.FilterListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.SearchFilter;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Task;

/**
 * Exposes Astrid's built in filters to the {@link FilterListActivity}
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class CoreFilterExposer extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Resources r = context.getResources();

        // core filters
        Filter inbox = buildInboxFilter(r);

        SearchFilter searchFilter = new SearchFilter(CorePlugin.IDENTIFIER,
                r.getString(R.string.BFE_Search));
        searchFilter.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.tango_search)).getBitmap();

        // extended
        FilterCategory extended = new FilterCategory(CorePlugin.IDENTIFIER,
                r.getString(R.string.BFE_Extended), new Filter[5]);

        Filter alphabetical = new Filter(r.getString(R.string.BFE_Alphabetical),
                r.getString(R.string.BFE_Alphabetical),
                new QueryTemplate().where(Criterion.and(TaskCriteria.isActive(),
                        TaskCriteria.isVisible(DateUtilities.now()))).
                        orderBy(Order.asc(Task.TITLE)),
                null);
        alphabetical.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.tango_alpha)).getBitmap();

        Filter recent = new Filter(r.getString(R.string.BFE_Recent),
                r.getString(R.string.BFE_Recent),
                new QueryTemplate().orderBy(Order.desc(Task.MODIFICATION_DATE)).limit(15),
                null);
        recent.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.tango_new)).getBitmap();

        ContentValues hiddenValues = new ContentValues();
        hiddenValues.put(Task.HIDE_UNTIL.name, DateUtilities.now() + DateUtilities.ONE_DAY);
        Filter hidden = new Filter(r.getString(R.string.BFE_Hidden),
                r.getString(R.string.BFE_Hidden),
                new QueryTemplate().where(Criterion.and(TaskCriteria.isActive(),
                        Criterion.not(TaskCriteria.isVisible(DateUtilities.now())))).
                        orderBy(Order.asc(Task.HIDE_UNTIL)),
                hiddenValues);
        hidden.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.tango_clouds)).getBitmap();

        ContentValues completedValues = new ContentValues();
        hiddenValues.put(Task.COMPLETION_DATE.name, DateUtilities.now());
        Filter completed = new Filter(r.getString(R.string.BFE_Completed), r.getString(R.string.BFE_Completed),
                new QueryTemplate().where(Criterion.and(TaskCriteria.completedBefore(DateUtilities.now()),
                        Criterion.not(TaskCriteria.isDeleted()))). orderBy(Order.desc(Task.COMPLETION_DATE)),
                completedValues);
        completed.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.tango_check)).getBitmap();

        Filter deleted = new Filter(r.getString(R.string.BFE_Deleted),
                r.getString(R.string.BFE_Deleted),
                new QueryTemplate().where(TaskCriteria.isDeleted()).
                        orderBy(Order.desc(Task.DELETION_DATE)),
                null);
        deleted.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.tango_trash)).getBitmap();

        extended.children[0] = alphabetical;
        extended.children[1] = recent;
        extended.children[2] = hidden;
        extended.children[3] = completed;
        extended.children[4] = deleted;

        // transmit filter list
        FilterListItem[] list = new FilterListItem[3];
        list[0] = inbox;
        list[1] = searchFilter;
        list[2] = extended;
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    /**
     * Build inbox filter
     * @return
     */
    public static Filter buildInboxFilter(Resources r) {
        Filter inbox = new Filter(r.getString(R.string.BFE_Active), r.getString(R.string.BFE_Active_title),
                new QueryTemplate().where(Criterion.and(TaskCriteria.isActive(),
                        TaskCriteria.isVisible(DateUtilities.now()))),
                null);
        inbox.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.tango_home)).getBitmap();
        return inbox;
    }

}
