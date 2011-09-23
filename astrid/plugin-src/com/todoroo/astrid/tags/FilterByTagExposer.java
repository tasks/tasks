package com.todoroo.astrid.tags;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ArrayAdapter;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.QueryTemplate;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.api.AstridApiConstants;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.TaskAction;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.tags.TagService.Tag;

public class FilterByTagExposer extends BroadcastReceiver {

    private static final String FILTER_ACTION = "com.todoroo.astrid.FILTER_BY_TAG"; //$NON-NLS-1$

    @Override
    public void onReceive(Context context, Intent intent) {
        ContextManager.setContext(context);
        long taskId = intent.getLongExtra(AstridApiConstants.EXTRAS_TASK_ID, -1);
        if(taskId == -1)
            return;

        TodorooCursor<Metadata> cursor = TagService.getInstance().getTags(taskId);
        try {
            if(cursor.getCount() == 0)
                return;
        } finally {
            cursor.close();
        }

        if(AstridApiConstants.BROADCAST_REQUEST_ACTIONS.equals(intent.getAction())) {
            final String label = context.getString(R.string.TAd_contextFilterByTag);
            final Drawable drawable = context.getResources().getDrawable(R.drawable.med_tag);
            Intent newIntent = new Intent(FILTER_ACTION);
            newIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            Bitmap icon = ((BitmapDrawable)drawable).getBitmap();
            TaskAction action = new TaskAction(label,
                    PendingIntent.getBroadcast(context, (int)taskId, newIntent, 0), icon);

            // transmit
            Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_SEND_ACTIONS);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_ADDON, TagsPlugin.IDENTIFIER);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_RESPONSE, action);
            broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
            context.sendBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
        } else if(FILTER_ACTION.equals(intent.getAction())) {
            invoke(taskId);
        }
    }

    public void invoke(long taskId) {
        final List<String> tags;
        final TodorooCursor<Metadata> cursor = TagService.getInstance().getTags(
                taskId);
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
                Tag tag = new Tag(tags.get(which), 0, 0);

                String listTitle = tag.tag;
                String title = tag.tag;
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
