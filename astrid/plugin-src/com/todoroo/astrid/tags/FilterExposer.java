/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.timsu.astrid.R;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.tags.TagService.Tag;

/**
 * Exposes filters based on tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterExposer extends BroadcastReceiver {

    private TagService tagService;

    @SuppressWarnings("nls")
    private Filter filterFromTag(Context context, Tag tag) {
        String listTitle = context.getString(R.string.tag_FEx_tag_w_size).
            replace("$T", tag.tag).replace("$C", Integer.toString(tag.count));
        String title = context.getString(R.string.tag_FEx_name, tag.tag);
        QueryTemplate tagTemplate = tag.queryTemplate();
        ContentValues contentValues = new ContentValues();
        contentValues.put(TagService.KEY, tag.tag);

        Filter filter = new Filter(TagsPlugin.IDENTIFIER,
                listTitle, title,
                tagTemplate,
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
        tagService = new TagService(context);
        Tag[] tagsByAlpha = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_ALPHA);

        // If user does not have any tags, don't show this section at all
        if(tagsByAlpha.length == 0)
            return;

        Tag[] tagsBySize = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE);
        Filter[] filtersByAlpha = new Filter[tagsByAlpha.length];
        for(int i = 0; i < tagsByAlpha.length; i++)
            filtersByAlpha[i] = filterFromTag(context, tagsByAlpha[i]);

        Filter[] filtersBySize = new Filter[tagsBySize.length];
        for(int i = 0; i < tagsBySize.length; i++)
            filtersBySize[i] = filterFromTag(context, tagsBySize[i]);

        FilterListHeader tagsHeader = new FilterListHeader(TagsPlugin.IDENTIFIER,
                context.getString(R.string.tag_FEx_header));
        FilterCategory tagsCategoryBySize = new FilterCategory(TagsPlugin.IDENTIFIER,
                context.getString(R.string.tag_FEx_by_size), filtersBySize);
        FilterCategory tagsCategoryByAlpha = new FilterCategory(TagsPlugin.IDENTIFIER,
                context.getString(R.string.tag_FEx_alpha), filtersByAlpha);

        // transmit filter list
        FilterListItem[] list = new FilterListItem[3];
        list[0] = tagsHeader;
        list[1] = tagsCategoryBySize;
        list[2] = tagsCategoryByAlpha;
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ITEMS, list);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

}
