package com.todoroo.astrid.tags;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.ArrayAdapter;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.TaskContextActionExposer;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.TagService.Tag;

public class FilterByTagContextAction implements TaskContextActionExposer {

    @Override
    public Object getLabel(Task task) {
        TodorooCursor<Metadata> cursor = TagService.getInstance().getTags(task.getId());
        try {
            if(cursor.getCount() > 0)
                return R.string.TAd_contextFilterByTag;
        } finally {
            cursor.close();
        }
        return null;
    }

    public void invoke(Task task) {
        final TodorooCursor<Metadata> cursor = TagService.getInstance().getTags(
                task.getId());
        final List<String> tags;
        try {
            if (!cursor.moveToFirst())
                return;
            tags = new ArrayList<String>();
            do {
                tags.add(cursor.get(TagService.TAG));
            } while (cursor.moveToNext());
        } finally {
            cursor.close();
        }

        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);
        Collections.sort(tags, collator);

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Tag tag = new Tag(tags.get(which), 0);

                String listTitle = tag.tag;
                String title = ContextManager.getString(
                        R.string.tag_FEx_name, tag.tag);
                Criterion criterion = TaskCriteria.activeAndVisible();
                QueryTemplate tagTemplate = tag.queryTemplate(criterion);
                ContentValues contentValues = new ContentValues();
                contentValues.put(Metadata.KEY.name, TagService.KEY);
                contentValues.put(TagService.TAG.name, tag.tag);

                Filter tagFilter = new Filter(listTitle, title,
                        tagTemplate, contentValues);
                Intent tagIntent = new Intent(ContextManager.getContext(),
                        TaskListActivity.class);
                tagIntent.putExtra(TaskListActivity.TOKEN_FILTER, tagFilter);

                ContextManager.getContext().startActivity(tagIntent);
                AndroidUtilities.callApiMethod(5,
                        this,
                        "overridePendingTransition", //$NON-NLS-1$
                        new Class<?>[] { Integer.TYPE, Integer.TYPE },
                        R.anim.slide_left_in, R.anim.slide_left_out);
            }
        };

        if (tags.size() == 1) {
            listener.onClick(null, 0);
        } else {
            new AlertDialog.Builder(ContextManager.getContext()).setTitle(
                    R.string.TAd_contextFilterByTag).setAdapter(
                    new ArrayAdapter<String>(ContextManager.getContext(),
                            android.R.layout.select_dialog_item, tags),
                    listener).show();
        }

    }

}
