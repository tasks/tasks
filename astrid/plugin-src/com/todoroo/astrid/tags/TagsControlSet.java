package com.todoroo.astrid.tags;

import java.util.ArrayList;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
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

    // --- constants

    /** Number of tags a task can have */
    static final int MAX_TAGS = 5;

    // --- instance variables

    private final TagService tagService = TagService.getInstance();
    private final Tag[] allTags;
    private final LinearLayout tagsContainer;
    private final Activity activity;

    public TagsControlSet(Activity activity, int tagsContainer) {
        allTags = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE);
        this.activity = activity;
        this.tagsContainer = (LinearLayout) activity.findViewById(tagsContainer);
    }

    @SuppressWarnings("nls")
    @Override
    public void readFromTask(Task task) {
        // tags (only configure if not already set)
        if(tagsContainer.getChildCount() == 0) {
            TodorooCursor<Metadata> cursor = tagService.getTags(task.getId());
            try {
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
                    addTag(cursor.get(TagService.TAG));
            } finally {
                cursor.close();
            }
            addTag("");
        }
    }

    @Override
    public void writeToModel(Task task) {
        ArrayList<String> tags = new ArrayList<String>();

        for(int i = 0; i < tagsContainer.getChildCount(); i++) {
            TextView tagName = (TextView)tagsContainer.getChildAt(i).findViewById(R.id.text1);
            if(tagName.getText().length() == 0)
                continue;
            tags.add(tagName.getText().toString());
        }

        tagService.synchronizeTags(task.getId(), tags);
    }

    /** Adds a tag to the tag field */
    boolean addTag(String tagName) {
        if (tagsContainer.getChildCount() >= MAX_TAGS) {
            return false;
        }

        LayoutInflater inflater = activity.getLayoutInflater();
        final View tagItem = inflater.inflate(R.layout.tag_edit_row, null);
        tagsContainer.addView(tagItem);

        AutoCompleteTextView textView = (AutoCompleteTextView)tagItem.
            findViewById(R.id.text1);
        textView.setText(tagName);
        ArrayAdapter<Tag> tagsAdapter =
            new ArrayAdapter<Tag>(activity,
                    android.R.layout.simple_dropdown_item_1line, allTags);
        textView.setAdapter(tagsAdapter);
        textView.addTextChangedListener(new TextWatcher() {
            @SuppressWarnings("nls")
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                if(start == 0 && tagsContainer.getChildAt(
                        tagsContainer.getChildCount()-1) == tagItem) {
                    addTag("");
                }
            }

            public void afterTextChanged(Editable s) {
                //
            }


            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                //
            }
        });

        ImageButton reminderRemoveButton;
        reminderRemoveButton = (ImageButton)tagItem.findViewById(R.id.button1);
        reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tagsContainer.removeView(tagItem);
            }
        });

        return true;
    }
}