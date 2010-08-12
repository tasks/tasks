/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;

import com.timsu.astrid.R;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.tags.TagService.Tag;

/**
 * Exposes filters based on tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagFilterExposer extends BroadcastReceiver {

    private TagService tagService;

    private Filter filterFromTag(Context context, Tag tag, Criterion criterion) {
        String listTitle = tag.tag;
        String title = context.getString(R.string.tag_FEx_name, tag.tag);
        QueryTemplate tagTemplate = tag.queryTemplate(criterion);
        ContentValues contentValues = new ContentValues();
        contentValues.put(Metadata.KEY.name, TagService.KEY);
        contentValues.put(TagService.TAG.name, tag.tag);

        Filter filter = new Filter(listTitle,
                title, tagTemplate,
                contentValues);

//        filters[0].contextMenuLabels = new String[] {
//            "Rename Tag",
//            "Delete Tag"
//        };
//        filters[0].contextMenuIntents = new Intent[] {
//                new Intent(),
//                new Intent()
//        };

        return filter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        tagService = TagService.getInstance();
        Tag[] tags = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE, TaskCriteria.notDeleted());

        // If user does not have any tags, don't show this section at all
        if(tags.length == 0)
            return;

        Resources r = context.getResources();

        FilterListItem[] list = new FilterListItem[3];

        FilterListHeader tagsHeader = new FilterListHeader(context.getString(R.string.tag_FEx_header));
        list[0] = tagsHeader;

        Filter untagged = new Filter(r.getString(R.string.tag_FEx_untagged),
                r.getString(R.string.tag_FEx_untagged),
                tagService.untaggedTemplate(),
                null);
        untagged.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.filter_untagged)).getBitmap();
        list[1] = untagged;


        Filter[] filters = new Filter[tags.length];
        for(int i = 0; i < tags.length; i++)
            filters[i] = filterFromTag(context, tags[i], TaskCriteria.isActive());
        FilterCategory tagsFilter = new FilterCategory(context.getString(R.string.tag_FEx_by_size), filters);
        list[2] = tagsFilter;


        // transmit filter list
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
