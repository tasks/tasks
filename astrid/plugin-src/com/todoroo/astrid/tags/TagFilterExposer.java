/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.FilterWithUpdate;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.core.SortHelper;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.subtasks.SubtasksTagListFragment;
import com.todoroo.astrid.tags.TagService.Tag;
import com.todoroo.astrid.utility.AstridPreferences;

/**
 * Exposes filters based on tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagFilterExposer extends BroadcastReceiver implements AstridFilterExposer {

    private static final String TAG = "tag"; //$NON-NLS-1$
    public static final String TAG_SQL = "tagSql"; //$NON-NLS-1$

    @Autowired TagDataService tagDataService;
    @Autowired GtasksPreferenceService gtasksPreferenceService;

    /** Create filter from new tag object */
    @SuppressWarnings("nls")
    public static FilterWithCustomIntent filterFromTag(Context context, Tag tag, Criterion criterion) {
        String title = tag.tag;
        QueryTemplate tagTemplate = tag.queryTemplate(criterion);
        ContentValues contentValues = new ContentValues();
        contentValues.put(Metadata.KEY.name, TagService.KEY);
        contentValues.put(TagService.TAG.name, tag.tag);

        FilterWithUpdate filter = new FilterWithUpdate(tag.tag,
                title, tagTemplate,
                contentValues);
        if(tag.remoteId > 0) {
            filter.listingTitle += " (" + tag.count + ")";
            if(tag.count == 0)
                filter.color = Color.GRAY;
        }

        TagData tagData = PluginServices.getTagDataService().getTag(tag.tag, TagData.ID,
                TagData.USER_ID, TagData.MEMBER_COUNT);
        int deleteIntentLabel;
        if (tagData != null && tagData.getValue(TagData.MEMBER_COUNT) > 0 && tagData.getValue(TagData.USER_ID) != 0)
            deleteIntentLabel = R.string.tag_cm_leave;
        else
            deleteIntentLabel = R.string.tag_cm_delete;

        filter.contextMenuLabels = new String[] {
            context.getString(R.string.tag_cm_rename),
            context.getString(deleteIntentLabel)
        };
        filter.contextMenuIntents = new Intent[] {
                newTagIntent(context, RenameTagActivity.class, tag, tagTemplate.toString()),
                newTagIntent(context, DeleteTagActivity.class, tag, tagTemplate.toString())
        };

        SharedPreferences publicPrefs = AstridPreferences.getPublicPrefs(context);
        int sortFlags = publicPrefs.getInt(SortHelper.PREF_SORT_FLAGS, 0);

        Class<?> fragmentClass = SortHelper.isManualSort(sortFlags) ?
                SubtasksTagListFragment.class : TagViewFragment.class;
        filter.customTaskList = new ComponentName(ContextManager.getContext(), fragmentClass);
        if(tag.image != null)
            filter.imageUrl = tag.image;
        if(tag.updateText != null)
            filter.updateText = tag.updateText;
        Bundle extras = new Bundle();
        extras.putString(TagViewFragment.EXTRA_TAG_NAME, tag.tag);
        extras.putLong(TagViewFragment.EXTRA_TAG_REMOTE_ID, tag.remoteId);
        extras.putBoolean(TaskListFragment.TOKEN_OVERRIDE_ANIM, true);
        filter.customExtras = extras;

        return filter;
    }

    /** Create a filter from tag data object */
    public static Filter filterFromTagData(Context context, TagData tagData) {
        Tag tag = new Tag(tagData.getValue(TagData.NAME),
                tagData.getValue(TagData.TASK_COUNT),
                tagData.getValue(TagData.REMOTE_ID));
        return filterFromTag(context, tag, TaskCriteria.activeAndVisible());
    }

    private static Intent newTagIntent(Context context, Class<? extends Activity> activity, Tag tag, String sql) {
        Intent ret = new Intent(context, activity);
        ret.putExtra(TAG, tag.tag);
        ret.putExtra(TAG_SQL, sql);
        return ret;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        FilterListItem[] listAsArray = prepareFilters(context);

        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, listAsArray);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, TagsPlugin.IDENTIFIER);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    private FilterListItem[] prepareFilters(Context context) {
        DependencyInjectionService.getInstance().inject(this);
        ContextManager.setContext(context);

        ArrayList<FilterListItem> list = new ArrayList<FilterListItem>();

        addTags(list);

        // transmit filter list
        FilterListItem[] listAsArray = list.toArray(new FilterListItem[list.size()]);
        return listAsArray;
    }

    private void addTags(ArrayList<FilterListItem> list) {
        ArrayList<Tag> tagList = TagService.getInstance().getTagList();
        list.add(filterFromTags(tagList.toArray(new Tag[tagList.size()]),
                R.string.tag_FEx_header));
    }

    private FilterCategory filterFromTags(Tag[] tags, int name) {
        Filter[] filters = new Filter[tags.length + 1];

        Context context = ContextManager.getContext();
        Resources r = context.getResources();

        // --- untagged
        int untaggedLabel = gtasksPreferenceService.isLoggedIn() ?
                R.string.tag_FEx_untagged_w_astrid : R.string.tag_FEx_untagged;
        Filter untagged = new Filter(r.getString(untaggedLabel),
                r.getString(R.string.tag_FEx_untagged),
                TagService.untaggedTemplate(),
                null);
        untagged.listingIcon = ((BitmapDrawable)r.getDrawable(R.drawable.gl_lists)).getBitmap();
        filters[0] = untagged;

        for(int i = 0; i < tags.length; i++)
            filters[i+1] = filterFromTag(context, tags[i], TaskCriteria.activeAndVisible());
        FilterCategory filter = new FilterCategory(context.getString(name), filters);
        return filter;
    }

    // --- tag manipulation activities

    public abstract static class TagActivity extends Activity {

        protected String tag;
        protected String sql;

        @Autowired public TagService tagService;
        @Autowired public TagDataService tagDataService;

        static {
            AstridDependencyInjector.initialize();
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            tag = getIntent().getStringExtra(TAG);
            sql = getIntent().getStringExtra(TAG_SQL);
            if(tag == null) {
                finish();
                return;
            }
            DependencyInjectionService.getInstance().inject(this);


            TagData tagData = tagDataService.getTag(tag, TagData.MEMBER_COUNT, TagData.USER_ID);
            if(tagData != null && tagData.getValue(TagData.MEMBER_COUNT) > 0 && tagData.getValue(TagData.USER_ID) == 0) {
                DialogUtilities.okCancelDialog(this, getString(R.string.actfm_tag_operation_owner_delete), getOkListener(), getCancelListener());
                return;
            }
            showDialog(tagData);
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

        protected abstract void showDialog(TagData tagData);

        protected abstract boolean ok();
    }

    public static class DeleteTagActivity extends TagActivity {

        @Override
        protected void showDialog(TagData tagData) {
            int string;
            if (tagData != null && tagData.getValue(TagData.MEMBER_COUNT) > 0)
                string = R.string.DLG_leave_this_shared_tag_question;
            else
                string = R.string.DLG_delete_this_tag_question;
            DialogUtilities.okCancelDialog(this, getString(string, tag), getOkListener(), getCancelListener());
        }

        @Override
        protected boolean ok() {
            int deleted = tagService.delete(tag);
            TagData tagData = PluginServices.getTagDataService().getTag(tag, TagData.ID, TagData.DELETION_DATE, TagData.MEMBER_COUNT, TagData.USER_ID);
            boolean shared = false;
            if(tagData != null) {
                tagData.setValue(TagData.DELETION_DATE, DateUtilities.now());
                PluginServices.getTagDataService().save(tagData);
                shared = tagData.getValue(TagData.MEMBER_COUNT) > 0 && tagData.getValue(TagData.USER_ID) != 0; // Was I a list member and NOT owner?
            }
            Toast.makeText(this, getString(shared ? R.string.TEA_tags_left : R.string.TEA_tags_deleted, tag, deleted),
                    Toast.LENGTH_SHORT).show();

            Intent tagDeleted = new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_DELETED);
            tagDeleted.putExtra(TagViewFragment.EXTRA_TAG_NAME, tag);
            tagDeleted.putExtra(TAG_SQL, sql);
            sendBroadcast(tagDeleted);
            return true;
        }

    }

    public static class RenameTagActivity extends TagActivity {

        private EditText editor;

        @Override
        protected void showDialog(TagData tagData) {
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
                TagData tagData = tagDataService.getTag(tag, TagData.ID, TagData.NAME);
                if (tagData != null) {
                    tagData.setValue(TagData.NAME, text);
                    tagDataService.save(tagData);
                }
                Toast.makeText(this, getString(R.string.TEA_tags_renamed, tag, text, renamed),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        }
    }

    @Override
    public FilterListItem[] getFilters() {
        if (ContextManager.getContext() == null)
            return null;

        return prepareFilters(ContextManager.getContext());
    }

}
