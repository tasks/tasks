/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.injection.ForApplication;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Exposes filters based on tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagFilterExposer {

    private final TagService tagService;
    private final Context context;

    @Inject
    public TagFilterExposer(@ForApplication Context context, TagService tagService) {
        this.context = context;
        this.tagService = tagService;
    }

    public List<Filter> getFilters() {
        ArrayList<Filter> list = new ArrayList<>();

        list.addAll(filterFromTags(tagService.getTagList()));

        // transmit filter list
        return list;
    }

    /** Create filter from new tag object */
    public static FilterWithCustomIntent filterFromTag(Context context, TagData tag, Criterion criterion) {
        String title = tag.getName();
        if (TextUtils.isEmpty(title)) {
            return null;
        }
        QueryTemplate tagTemplate = queryTemplate(tag.getUuid(), criterion);
        ContentValues contentValues = new ContentValues();
        contentValues.put(Metadata.KEY.name, TaskToTagMetadata.KEY);
        contentValues.put(TaskToTagMetadata.TAG_NAME.name, tag.getName());
        contentValues.put(TaskToTagMetadata.TAG_UUID.name, tag.getUuid());

        FilterWithCustomIntent filter = new FilterWithCustomIntent(title, tagTemplate, contentValues);

        filter.customTaskList = new ComponentName(context, TagViewFragment.class);
        Bundle extras = new Bundle();
        extras.putString(TagViewFragment.EXTRA_TAG_NAME, tag.getName());
        extras.putString(TagViewFragment.EXTRA_TAG_UUID, tag.getUuid());
        filter.customExtras = extras;

        return filter;
    }

    /** Create a filter from tag data object */
    public static FilterWithCustomIntent filterFromTagData(Context context, TagData tagData) {
        return filterFromTag(context, tagData, TaskCriteria.activeAndVisible());
    }

    private List<Filter> filterFromTags(List<TagData> tags) {

        List<Filter> filters = new ArrayList<>();

        int label = R.drawable.ic_label_24dp;

        for (TagData tag : tags) {
            Filter f = constructFilter(context, tag);
            if (f != null) {
                f.icon = label;
                filters.add(f);
            }
        }
        return filters;
    }

    protected Filter constructFilter(Context context, TagData tag) {
        return filterFromTag(context, tag, TaskCriteria.activeAndVisible());
    }

    private static QueryTemplate queryTemplate(String uuid, Criterion criterion) {
        Criterion fullCriterion = Criterion.and(
                Field.field("mtags." + Metadata.KEY.name).eq(TaskToTagMetadata.KEY),
                Field.field("mtags." + TaskToTagMetadata.TAG_UUID.name).eq(uuid),
                Field.field("mtags." + Metadata.DELETION_DATE.name).eq(0),
                criterion);
        return new QueryTemplate().join(Join.inner(Metadata.TABLE.as("mtags"), Task.UUID.eq(Field.field("mtags." + TaskToTagMetadata.TASK_UUID.name))))
                .where(fullCriterion);
    }
}
