/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Field;
import com.todoroo.andlib.sql.Join;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.FilterWithUpdate;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;

import org.tasks.R;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.injection.Injector;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Exposes filters based on tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagFilterExposer extends InjectingBroadcastReceiver implements AstridFilterExposer {

    public static final String TAG = "tag"; //$NON-NLS-1$

    @Inject TagService tagService;
    @Inject @ForApplication Context context;
    @Inject Preferences preferences;

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

        FilterWithUpdate filter = new FilterWithUpdate(tag.getName(),
                title, tagTemplate,
                contentValues);

        filter.contextMenuLabels = new String[] {
            context.getString(R.string.tag_cm_rename),
            context.getString(R.string.tag_cm_delete)
        };
        filter.contextMenuIntents = new Intent[] {
                newTagIntent(context, RenameTagActivity.class, tag, tag.getUuid()),
                newTagIntent(context, DeleteTagActivity.class, tag, tag.getUuid())
        };

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

    private static Intent newTagIntent(Context context, Class<? extends Activity> activity, TagData tag, String uuid) {
        Intent ret = new Intent(context, activity);
        ret.putExtra(TAG, tag.getName());
        ret.putExtra(TagViewFragment.EXTRA_TAG_UUID, uuid);
        return ret;
    }

    private FilterListItem[] prepareFilters() {
        ContextManager.setContext(context);

        ArrayList<FilterListItem> list = new ArrayList<>();

        list.addAll(filterFromTags(tagService.getTagList()));

        // transmit filter list
        return list.toArray(new FilterListItem[list.size()]);
    }

    private List<Filter> filterFromTags(List<TagData> tags) {
        boolean shouldAddUntagged = preferences.getBoolean(R.string.p_show_not_in_list_filter, true);

        List<Filter> filters = new ArrayList<>();

        Resources r = context.getResources();

        // --- untagged
        if (shouldAddUntagged) {
            Filter untagged = new Filter(r.getString(R.string.tag_FEx_untagged),
                    r.getString(R.string.tag_FEx_untagged),
                    untaggedTemplate(),
                    null);
            filters.add(untagged);
        }

        for (TagData tag : tags) {
            Filter f = constructFilter(context, tag);
            if (f != null) {
                filters.add(f);
            }
        }
        return filters;
    }

    protected Filter constructFilter(Context context, TagData tag) {
        return filterFromTag(context, tag, TaskCriteria.activeAndVisible());
    }

    @Override
    public FilterListItem[] getFilters(Injector injector) {
        injector.inject(this);

        return prepareFilters();
    }

    private QueryTemplate untaggedTemplate() {
        return new QueryTemplate().where(Criterion.and(
                Criterion.not(Task.UUID.in(Query.select(TaskToTagMetadata.TASK_UUID).from(Metadata.TABLE)
                        .where(Criterion.and(MetadataDao.MetadataCriteria.withKey(TaskToTagMetadata.KEY), Metadata.DELETION_DATE.eq(0))))),
                TaskCriteria.isActive(),
                TaskCriteria.isVisible()));
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
