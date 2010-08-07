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

    @SuppressWarnings("nls")
    private Filter filterFromTag(Context context, Tag tag, boolean completed) {
        String listTitle = context.getString(R.string.tag_FEx_tag_w_size).
            replace("$T", tag.tag).replace("$C", Integer.toString(tag.count));
        String title = context.getString(R.string.tag_FEx_name, tag.tag);
        QueryTemplate tagTemplate = tag.queryTemplate(completed);
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
        Tag[] tagsByAlpha = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_ALPHA, Criterion.all);

        // If user does not have any tags, don't show this section at all
        if(tagsByAlpha.length == 0)
            return;

        Resources r = context.getResources();

        Tag[] tagsBySize = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE, TaskCriteria.isActive());
        Filter[] filtersByAlpha = new Filter[tagsByAlpha.length];
        for(int i = 0; i < tagsByAlpha.length; i++)
            filtersByAlpha[i] = filterFromTag(context, tagsByAlpha[i], false);

        Filter[] filtersBySize = new Filter[tagsBySize.length];
        for(int i = 0; i < tagsBySize.length; i++)
            filtersBySize[i] = filterFromTag(context, tagsBySize[i], false);

        Tag[] completed = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE, TaskCriteria.completed());
        Filter[] filtersCompleted = new Filter[completed.length];
        for(int i = 0; i < completed.length; i++)
            filtersCompleted[i] = filterFromTag(context, completed[i], true);

        FilterListHeader tagsHeader = new FilterListHeader(context.getString(R.string.tag_FEx_header));

        Filter untagged = new Filter(r.getString(R.string.tag_FEx_untagged),
                r.getString(R.string.tag_FEx_untagged),
                tagService.untaggedTemplate(),
                null);
        untagged.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.filter_untagged)).getBitmap();

        FilterCategory tagsCategoryBySize = new FilterCategory(context.getString(R.string.tag_FEx_by_size),
                filtersBySize);
        tagsCategoryBySize.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.filter_tags1)).getBitmap();

        FilterCategory tagsCategoryByAlpha = new FilterCategory(context.getString(R.string.tag_FEx_alpha),
                filtersByAlpha);
        tagsCategoryByAlpha.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.filter_tags1)).getBitmap();

        FilterCategory tagsCategoryCompleted = new FilterCategory(context.getString(R.string.tag_FEx_completed),
                filtersCompleted);
        tagsCategoryCompleted.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.filter_tags2)).getBitmap();

        // transmit filter list
        FilterListItem[] list = new FilterListItem[5];
        list[0] = tagsHeader;
        list[1] = untagged;
        list[2] = tagsCategoryBySize;
        list[3] = tagsCategoryByAlpha;
        list[4] = tagsCategoryCompleted;
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
