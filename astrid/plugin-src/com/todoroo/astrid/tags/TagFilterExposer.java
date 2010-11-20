/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterCategory;
import com.todoroo.astrid.api.FilterListHeader;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.tags.TagService.Tag;

/**
 * Exposes filters based on tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagFilterExposer extends BroadcastReceiver {

    private static final String TAG = "tag";

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

        return filter;
    }

    private Intent newTagIntent(Context context, Class<? extends Activity> activity, Tag tag) {
        Intent ret = new Intent(context, activity);
        ret.putExtra(TAG, tag.tag);
        return ret;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        tagService = TagService.getInstance();
        Tag[] tags = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE, TaskCriteria.notDeleted());

        // If user does not have any tags, don't show this section at all
        if(tags.length == 0)
            return;

        // sort tags by # of active tasks
        Tag[] activeTags = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE, TaskCriteria.activeAndVisible());
        HashMap<String, Integer> actives = new HashMap<String, Integer>();
        for(Tag tag : activeTags)
            actives.put(tag.tag, tag.count);
        TreeSet<Tag> sortedTagSet = new TreeSet<Tag>(new Comparator<Tag>() {
            @Override
            public int compare(Tag a, Tag b) {
                if(a.count == b.count)
                    return a.tag.compareTo(b.tag);
                return b.count - a.count;
            }
        });
        for(Tag tag : tags) {
            if(!actives.containsKey(tag.tag))
                tag.count = 0;
            else {
                // will decrease tag.count is there are tasks with this tag which are not activeAndVisible but also have not been deleted
                tag.count = actives.get(tag.tag);
            }
            sortedTagSet.add(tag);
        }

        // create filter list
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


        Filter[] filters = new Filter[sortedTagSet.size()];
        int index = 0;
        for(Tag tag : sortedTagSet) {
            filters[index++] = filterFromTag(context, tag, TaskCriteria.activeAndVisible());
        }
        FilterCategory tagsFilter = new FilterCategory(context.getString(R.string.tag_FEx_by_size), filters);
        list[2] = tagsFilter;

        // transmit filter list
        Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_FILTERS);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, list);
        broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, TagsPlugin.IDENTIFIER);
        context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
    }

    public abstract static class TagActivity extends Activity {

        protected String tag;

        @Autowired public TagService tagService;

        static {
            AstridDependencyInjector.initialize();
        }

        protected TagActivity() {
            DependencyInjectionService.getInstance().inject(this);
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

            DependencyInjectionService.getInstance().inject(this); // why?

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
            Toast.makeText(this, getString(R.string.TEA_tags_deleted, tag, deleted),
                    Toast.LENGTH_SHORT).show();
            return true;
        }

    }

    public static class RenameTagActivity extends TagActivity {

        private EditText editor;

        @Override
        protected void showDialog() {
            editor = new EditText(this); // not sure why this can't be done in the RenameTagActivity constructor.
            DialogUtilities.viewDialog(this, getString(R.string.DLG_rename_this_tag_header, tag), editor, getOkListener(), getCancelListener());
        }

        @Override
        protected boolean ok() { // this interface is not going to work well with the dialog that says "Are you sure?"
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
