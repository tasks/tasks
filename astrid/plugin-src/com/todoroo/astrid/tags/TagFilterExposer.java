/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.AstridFilterExposer;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithCustomIntent;
import com.todoroo.astrid.api.FilterWithUpdate;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TagMetadataDao;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TagService.Tag;

/**
 * Exposes filters based on tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagFilterExposer extends BroadcastReceiver implements AstridFilterExposer {

    private static final String TAG = "tag"; //$NON-NLS-1$

    @Autowired protected TagDataService tagDataService;
    @Autowired GtasksPreferenceService gtasksPreferenceService;

    protected boolean addUntaggedFilter = true;

    /** Create filter from new tag object */
    @SuppressWarnings("nls")
    public static FilterWithCustomIntent filterFromTag(Context context, Tag tag, Criterion criterion) {
        String title = tag.tag;
        if (TextUtils.isEmpty(title))
            return null;
        QueryTemplate tagTemplate = tag.queryTemplate(criterion);
        ContentValues contentValues = new ContentValues();
        contentValues.put(Metadata.KEY.name, TaskToTagMetadata.KEY);
        contentValues.put(TaskToTagMetadata.TAG_NAME.name, tag.tag);
        contentValues.put(TaskToTagMetadata.TAG_UUID.name, tag.uuid);

        FilterWithUpdate filter = new FilterWithUpdate(tag.tag,
                title, tagTemplate,
                contentValues);
        if(!RemoteModel.NO_UUID.equals(tag.uuid)) {
            filter.listingTitle += " (" + tag.count + ")";
        }

        int deleteIntentLabel;
        if (tag.memberCount > 0 && !Task.USER_ID_SELF.equals(tag.userId))
            deleteIntentLabel = R.string.tag_cm_leave;
        else
            deleteIntentLabel = R.string.tag_cm_delete;

        filter.contextMenuLabels = new String[] {
            context.getString(R.string.tag_cm_rename),
            context.getString(deleteIntentLabel)
        };
        filter.contextMenuIntents = new Intent[] {
                newTagIntent(context, RenameTagActivity.class, tag, tag.uuid),
                newTagIntent(context, DeleteTagActivity.class, tag, tag.uuid)
        };

        filter.customTaskList = new ComponentName(ContextManager.getContext(), TagViewFragment.class);
        if(tag.image != null)
            filter.imageUrl = tag.image;
        Bundle extras = new Bundle();
        extras.putString(TagViewFragment.EXTRA_TAG_NAME, tag.tag);
        extras.putString(TagViewFragment.EXTRA_TAG_UUID, tag.uuid.toString());
        filter.customExtras = extras;

        return filter;
    }

    /** Create a filter from tag data object */
    public static FilterWithCustomIntent filterFromTagData(Context context, TagData tagData) {
        Tag tag = new Tag(tagData);
        return filterFromTag(context, tag, TaskCriteria.activeAndVisible());
    }

    private static Intent newTagIntent(Context context, Class<? extends Activity> activity, Tag tag, String uuid) {
        Intent ret = new Intent(context, activity);
        ret.putExtra(TAG, tag.tag);
        ret.putExtra(TagViewFragment.EXTRA_TAG_UUID, uuid);
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

    protected FilterListItem[] prepareFilters(Context context) {
        DependencyInjectionService.getInstance().inject(this);
        ContextManager.setContext(context);

        ArrayList<FilterListItem> list = new ArrayList<FilterListItem>();

        addTags(list);

        // transmit filter list
        FilterListItem[] listAsArray = list.toArray(new FilterListItem[list.size()]);
        return listAsArray;
    }

    private void addTags(ArrayList<FilterListItem> list) {
        List<Tag> tagList = getTagList();
        list.add(filterFromTags(tagList.toArray(new Tag[tagList.size()]),
                R.string.tag_FEx_header));
    }

    protected List<Tag> getTagList() {
        return TagService.getInstance().getTagList();
    }

    private FilterCategory filterFromTags(Tag[] tags, int name) {
        boolean shouldAddUntagged = addUntaggedFilter &&
                Preferences.getBoolean(R.string.p_show_not_in_list_filter, true);

        ArrayList<Filter> filters = new ArrayList<Filter>(tags.length);

        Context context = ContextManager.getContext();
        Resources r = context.getResources();

        int themeFlags = ThemeService.getFilterThemeFlags();

        // --- untagged
        if (shouldAddUntagged) {
            Filter untagged = new Filter(r.getString(R.string.tag_FEx_untagged),
                    r.getString(R.string.tag_FEx_untagged),
                    TagService.getInstance().untaggedTemplate(),
                    null);
            untagged.listingIcon = ((BitmapDrawable)r.getDrawable(
                    ThemeService.getDrawable(R.drawable.gl_lists, themeFlags))).getBitmap();
            filters.add(untagged);
        }

        for(int i = 0; i < tags.length; i++) {
            Filter f = constructFilter(context, tags[i]);
            if (f != null)
                filters.add(f);
        }
        FilterCategory filter = new FilterCategory(context.getString(name), filters.toArray(new Filter[filters.size()]));
        return filter;
    }

    protected Filter constructFilter(Context context, Tag tag) {
        return filterFromTag(context, tag, TaskCriteria.activeAndVisible());
    }

    // --- tag manipulation activities

    public abstract static class TagActivity extends Activity {

        protected String tag;
        protected String uuid;

        @Autowired public TagService tagService;
        @Autowired public TagDataDao tagDataDao;
        @Autowired public TagMetadataDao tagMetadataDao;

        static {
            AstridDependencyInjector.initialize();
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            tag = getIntent().getStringExtra(TAG);
            uuid = getIntent().getStringExtra(TagViewFragment.EXTRA_TAG_UUID);

            if(tag == null || RemoteModel.isUuidEmpty(uuid)) {
                finish();
                return;
            }
            DependencyInjectionService.getInstance().inject(this);


            TagData tagData = tagDataDao.fetch(uuid, TagData.MEMBER_COUNT, TagData.USER_ID);
            showDialog(tagData);
        }

        protected DialogInterface.OnClickListener getOkListener() {
            return new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        Intent result = ok();
                        if (result != null) {
                            setResult(RESULT_OK, result);
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

        protected abstract Intent ok();
    }

    public static class DeleteTagActivity extends TagActivity {

        @Override
        protected void showDialog(TagData tagData) {
            int string;
            if (tagData != null && (tagMetadataDao.tagHasMembers(uuid) || tagData.getValue(TagData.MEMBER_COUNT) > 0)) {
                if (Task.USER_ID_SELF.equals(tagData.getValue(TagData.USER_ID)))
                    string = R.string.actfm_tag_operation_owner_delete;
                else
                    string = R.string.DLG_leave_this_shared_tag_question;
            }
            else
                string = R.string.DLG_delete_this_tag_question;
            DialogUtilities.okCancelDialog(this, getString(string, tag), getOkListener(), getCancelListener());
        }

        @Override
        protected Intent ok() {
            return tagService.deleteOrLeaveTag(this, tag, uuid);
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
        protected Intent ok() {
            if(editor == null)
                return null;

            String text = editor.getText().toString();
            if (text == null || text.length() == 0) {
                return null;
            } else {
                int renamed = tagService.rename(uuid, text);
                Toast.makeText(this, getString(R.string.TEA_tags_renamed, tag, text, renamed),
                        Toast.LENGTH_SHORT).show();

                if (renamed > 0) {
                    Intent intent = new Intent(AstridApiConstants.BROADCAST_EVENT_TAG_RENAMED);
                    intent.putExtra(TagViewFragment.EXTRA_TAG_UUID, uuid);
                    ContextManager.getContext().sendBroadcast(intent);
                    return intent;
                }
                return null;
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
