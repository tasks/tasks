package com.todoroo.astrid.tags;

import java.util.LinkedHashSet;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.model.Metadata;
import com.todoroo.astrid.model.Task;
import com.todoroo.astrid.tags.TagService.Tag;

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TagsControlSet implements TaskEditControlSet {

    // --- instance variables

    private final TagService tagService = TagService.getInstance();
    private final Tag[] allTags;
    private String[] loadedTags;
    private final LinearLayout tagsContainer;
    private final Activity activity;

    public TagsControlSet(Activity activity, int tagsContainer) {
        allTags = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE, Criterion.all);
        this.activity = activity;
        this.tagsContainer = (LinearLayout) activity.findViewById(tagsContainer);
    }

    @Override
    public void readFromTask(Task task) {
        tagsContainer.removeAllViews();

        if(task.getId() != AbstractModel.NO_ID) {
            TodorooCursor<Metadata> cursor = tagService.getTags(task.getId());
            try {
                loadedTags = new String[cursor.getCount()];
                for(int i = 0; i < loadedTags.length; i++) {
                    cursor.moveToNext();
                    String tag = cursor.get(TagService.TAG);
                    addTag(tag);
                    loadedTags[i] = tag;
                }
            } finally {
                cursor.close();
            }
        }

        if(tagsContainer.getChildCount() == 0)
            addTag(""); //$NON-NLS-1$
    }

    @Override
    public void writeToModel(Task task) {
        // this is a case where we're asked to save but the UI was not yet populated
        if(tagsContainer.getChildCount() == 0)
            return;

        LinkedHashSet<String> tags = new LinkedHashSet<String>();
        boolean identical = true;

        for(int i = 0; i < tagsContainer.getChildCount(); i++) {
            TextView tagName = (TextView)tagsContainer.getChildAt(i).findViewById(R.id.text1);
            if(tagName.getText().length() == 0)
                continue;
            String tag = tagName.getText().toString();
            tags.add(tag);

            if(loadedTags.length <= i || !loadedTags[i].equals(tag))
                identical = false;
        }
        if(identical && tags.size() != loadedTags.length)
            identical = false;

        if(!identical) {
            tagService.synchronizeTags(task.getId(), tags);
            task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        }
    }

    /** Adds a tag to the tag field */
    boolean addTag(String tagName) {
        LayoutInflater inflater = activity.getLayoutInflater();
        final View tagItem = inflater.inflate(R.layout.tag_edit_row, null);
        tagsContainer.addView(tagItem);

        final AutoCompleteTextView textView = (AutoCompleteTextView)tagItem.
            findViewById(R.id.text1);
        textView.setText(tagName);
        ArrayAdapter<Tag> tagsAdapter =
            new ArrayAdapter<Tag>(activity,
                    android.R.layout.simple_dropdown_item_1line, allTags);
        textView.setAdapter(tagsAdapter);

        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                View lastItem = tagsContainer.getChildAt(tagsContainer.getChildCount()-1);
                TextView lastText = (TextView) lastItem.findViewById(R.id.text1);
                if(lastText.getText().length() != 0) {
                    addTag(""); //$NON-NLS-1$
                }
            }
        });

        /*textView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent arg2) {
                if(actionId != EditorInfo.IME_NULL)
                    return false;
                View lastItem = tagsContainer.getChildAt(tagsContainer.getChildCount()-1);
                TextView lastText = (TextView) lastItem.findViewById(R.id.text1);
                if(lastText.getText().length() != 0) {
                    addTag(""); //$NON-NLS-1$
                }
                return true;
            }
        });*/

        ImageButton reminderRemoveButton;
        reminderRemoveButton = (ImageButton)tagItem.findViewById(R.id.button1);
        reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(tagsContainer.getChildCount() > 0)
                    tagsContainer.removeView(tagItem);
                else
                    textView.setText(""); //$NON-NLS-1$
            }
        });

        return true;
    }
}