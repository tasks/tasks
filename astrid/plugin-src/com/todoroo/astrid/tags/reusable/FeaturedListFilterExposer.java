package com.todoroo.astrid.tags.reusable;

import java.util.List;

import android.content.Context;
import android.content.Intent;

import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagService;
import com.todoroo.astrid.tags.TagService.Tag;

public class FeaturedListFilterExposer extends TagFilterExposer {

    @Override
    public void onReceive(Context context, Intent intent) {
        FilterListItem[] listAsArray = prepareFilters(context);

        Intent broadcastIntent = new Intent(FeaturedListFilterAdapter.BROADCAST_SEND_FEATURED_LISTS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, listAsArray);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    @Override
    protected List<Tag> getTagList() {
        return TagService.getInstance().getFeaturedLists();
    }

}
