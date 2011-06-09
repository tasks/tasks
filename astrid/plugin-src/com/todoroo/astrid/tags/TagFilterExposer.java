/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import java.util.ArrayList;
import java.util.HashSet;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.TagViewActivity;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.tags.TagService.Tag;

/**
 * Exposes filters based on tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagFilterExposer extends BroadcastReceiver {

    private static final String TAG = "tag"; //$NON-NLS-1$

    @Autowired TagDataService tagDataService;

    @Autowired ActFmPreferenceService actFmPreferenceService;

    private TagService tagService;

    /** Create filter from new tag object */
    @SuppressWarnings("nls")
    public static Filter filterFromTag(Context context, Tag tag, Criterion criterion, boolean useTagViewActivity) {
        String listTitle = tag.tag + " (" + tag.count + ")";
        String title = context.getString(R.string.tag_FEx_name, tag.tag);
        QueryTemplate tagTemplate = tag.queryTemplate(criterion);
        ContentValues contentValues = new ContentValues();
        contentValues.put(Metadata.KEY.name, TagService.KEY);
        contentValues.put(TagService.TAG.name, tag.tag);

        FilterWithCustomIntent filter = new FilterWithCustomIntent(listTitle,
                title, tagTemplate,
                contentValues);
        if(tag.count == 0)
            filter.color = Color.GRAY;

        filter.contextMenuLabels = new String[] {
            context.getString(R.string.tag_cm_rename),
            context.getString(R.string.tag_cm_delete)
        };
        filter.contextMenuIntents = new Intent[] {
                newTagIntent(context, RenameTagActivity.class, tag),
                newTagIntent(context, DeleteTagActivity.class, tag)
        };
        if(useTagViewActivity) {
            filter.customTaskList = new ComponentName(ContextManager.getContext(), TagViewActivity.class);
            Bundle extras = new Bundle();
            extras.putString(TagViewActivity.EXTRA_TAG_NAME, tag.tag);
            extras.putLong(TagViewActivity.EXTRA_TAG_REMOTE_ID, tag.remoteId);
            filter.customExtras = extras;
        } else {
            filter.customTaskList = new ComponentName(ContextManager.getContext(), TaskListActivity.class);
        }

        return filter;
    }

    /** Create a filter from tag data object */
    public static Filter filterFromTagData(Context context, TagData tagData) {
        Tag tag = new Tag(tagData.getValue(TagData.NAME),
                tagData.getValue(TagData.TASK_COUNT),
                tagData.getValue(TagData.REMOTE_ID));
        return filterFromTag(context, tag, TaskCriteria.activeAndVisible(), true);
    }

    private static Intent newTagIntent(Context context, Class<? extends Activity> activity, Tag tag) {
        Intent ret = new Intent(context, activity);
        ret.putExtra(TAG, tag.tag);
        return ret;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        DependencyInjectionService.getInstance().inject(this);
        ContextManager.setContext(context);
        tagService = TagService.getInstance();

        Resources r = context.getResources();
        ArrayList<FilterListItem> list = new ArrayList<FilterListItem>();

        // --- header
        FilterListHeader tagsHeader = new FilterListHeader(context.getString(R.string.tag_FEx_header));
        list.add(tagsHeader);

        // --- untagged
        Filter untagged = new Filter(r.getString(R.string.tag_FEx_untagged),
                r.getString(R.string.tag_FEx_untagged),
                tagService.untaggedTemplate(),
                null);
        untagged.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.filter_untagged)).getBitmap();
        list.add(untagged);

        addTags(list);

        // transmit filter list
        if(list.size() <= 2)
            return;
        FilterListItem[] listAsArray = list.toArray(new FilterListItem[list.size()]);
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, listAsArray);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, TagsPlugin.IDENTIFIER);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private void addTags(ArrayList<FilterListItem> list) {
        HashSet<String> tagNames = new HashSet<String>();

        // active tags
        Tag[] myTags = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE,
                Criterion.and(TaskCriteria.ownedByMe(), TaskCriteria.activeAndVisible()));
        for(Tag tag : myTags)
            tagNames.add(tag.tag);
        if(myTags.length > 0)
            list.add(filterFromTags(myTags, R.string.tag_FEx_category_mine));

        // find all tag data not in active tag list
        TodorooCursor<TagData> cursor = tagDataService.query(Query.select(
                TagData.NAME, TagData.TASK_COUNT, TagData.REMOTE_ID).where(TagData.DELETION_DATE.eq(0)));
        ArrayList<Tag> notListed = new ArrayList<Tag>();
        try {
            ArrayList<Tag> sharedTags = new ArrayList<Tag>();
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String tagName = cursor.get(TagData.NAME);
                if(tagNames.contains(tagName))
                    continue;
                Tag tag = new Tag(tagName, cursor.get(TagData.TASK_COUNT),
                        cursor.get(TagData.REMOTE_ID));
                if(tag.count > 0)
                    sharedTags.add(tag);
                else
                    notListed.add(tag);
                tagNames.add(tagName);
            }
            if(sharedTags.size() > 0)
                list.add(filterFromTags(sharedTags.toArray(new Tag[sharedTags.size()]), R.string.tag_FEx_category_shared));
        } finally {
            cursor.close();
        }

        // find inactive tags, intersect tag list
        Tag[] inactiveTags = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_ALPHA,
                Criterion.and(TaskCriteria.notDeleted(), Criterion.not(TaskCriteria.activeAndVisible())));
        for(Tag tag : inactiveTags) {
            if(!tagNames.contains(tag.tag) && !TextUtils.isEmpty(tag.tag)) {
                notListed.add(tag);
                tag.count = 0;
            }
        }
        if(notListed.size() > 0)
            list.add(filterFromTags(notListed.toArray(new Tag[notListed.size()]),
                    R.string.tag_FEx_category_inactive));
    }

    private FilterCategory filterFromTags(Tag[] tags, int name) {
        Filter[] filters = new Filter[tags.length];
        Context context = ContextManager.getContext();
        for(int i = 0; i < tags.length; i++)
            filters[i] = filterFromTag(context, tags[i], TaskCriteria.activeAndVisible(),
                    actFmPreferenceService.isLoggedIn());
        return new FilterCategory(context.getString(name), filters);
    }

    // --- tag manipulation activities

    public abstract static class TagActivity extends Activity {

        protected String tag;

        @Autowired public TagService tagService;
        @Autowired public TagDataService tagDataService;

        static {
            AstridDependencyInjector.initialize();
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTheme(android.R.style.Theme_Dialog);

            tag = getIntent().getStringExtra(TAG);
            if(tag == null) {
                finish();
                return;
            }
            DependencyInjectionService.getInstance().inject(this);


            TagData tagData = tagDataService.getTag(tag, TagData.MEMBER_COUNT);
            if(tagData != null && tagData.getValue(TagData.MEMBER_COUNT) > 0) {
                DialogUtilities.okDialog(this, getString(R.string.actfm_tag_operation_disabled), getCancelListener());
                return;
            }
            showDialog();
        }

        protected DialogInterface.OnClickListener getOkListener() {
            return new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        if (ok()) {
                            setResult(RESULT_OK);
                        } else {
                            toastNoChanges();
                            setResult(RESULT_CANCELED);
                        }
                    } finally {
                        finish();
                    }
                }
            };
        }

        protected DialogInterface.OnClickListener getCancelListener() {
            return new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        toastNoChanges();
                    } finally {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }

            };
        }

        private void toastNoChanges() {
            Toast.makeText(this, R.string.TEA_no_tags_modified,
                        Toast.LENGTH_SHORT).show();
        }

        protected abstract void showDialog();

        protected abstract boolean ok();
    }

    public static class DeleteTagActivity extends TagActivity {

        @Override
        protected void showDialog() {
            DialogUtilities.okCancelDialog(this, getString(R.string.DLG_delete_this_tag_question, tag), getOkListener(), getCancelListener());
        }

        @Override
        protected boolean ok() {
            int deleted = tagService.delete(tag);
            TagData tagData = PluginServices.getTagDataService().getTag(tag, TagData.ID, TagData.DELETION_DATE);
            tagData.setValue(TagData.DELETION_DATE, DateUtilities.now());
            PluginServices.getTagDataService().save(tagData);
            Toast.makeText(this, getString(R.string.TEA_tags_deleted, tag, deleted),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

    }

    public static class RenameTagActivity extends TagActivity {

        private EditText editor;

        @Override
        protected void showDialog() {
            editor = new EditText(this);
            DialogUtilities.viewDialog(this, getString(R.string.DLG_rename_this_tag_header, tag), editor, getOkListener(), getCancelListener());
        }

        @Override
        protected boolean ok() {
            if(editor == null)
                return false;

            String text = editor.getText().toString();
            if (text == null || text.length() == 0) {
                return false;
            } else {
                int renamed = tagService.rename(tag, text);
                Toast.makeText(this, getString(R.string.TEA_tags_renamed, tag, text, renamed),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        }
    }

}
