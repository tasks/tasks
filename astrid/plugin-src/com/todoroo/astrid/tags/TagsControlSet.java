package com.todoroo.astrid.tags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.activity.TaskEditActivity.TaskEditControlSet;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.tags.TagService.Tag;
import com.todoroo.astrid.utility.Flags;

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TagsControlSet implements TaskEditControlSet {

    // --- instance variables

    private final Spinner tagSpinner;
    private final TagService tagService = TagService.getInstance();
    private final Tag[] allTags;
    private final LinearLayout tagsContainer;
    private final Activity activity;

    public TagsControlSet(Activity activity, int tagsContainer) {
        allTags = tagService.getGroupedTags(TagService.GROUPED_TAGS_BY_SIZE, Criterion.all);
        this.activity = activity;
        this.tagsContainer = (LinearLayout) activity.findViewById(tagsContainer);
        this.tagSpinner = (Spinner) activity.findViewById(R.id.tags_dropdown);

        if(allTags.length == 0) {
            tagSpinner.setVisibility(View.GONE);
        } else {
            ArrayList<Tag> dropDownList = new ArrayList<Tag>(Arrays.asList(allTags));
            dropDownList.add(0, new Tag(activity.getString(R.string.TEA_tag_dropdown), 0, 0));
            ArrayAdapter<Tag> tagAdapter = new ArrayAdapter<Tag>(activity,
                    android.R.layout.simple_spinner_item,
                    dropDownList);
            tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            tagSpinner.setAdapter(tagAdapter);
            tagSpinner.setSelection(0);

            tagSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                        int position, long arg3) {
                    if(position == 0 || position > allTags.length)
                        return;
                    addTag(allTags[position - 1].tag, true);
                    tagSpinner.setSelection(0);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    // nothing!
                }
            });
        }
    }

    @Override
    public void readFromTask(Task task) {
        tagsContainer.removeAllViews();

        if(task.getId() != AbstractModel.NO_ID) {
            TodorooCursor<Metadata> cursor = tagService.getTags(task.getId());
            try {
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    String tag = cursor.get(TagService.TAG);
                    addTag(tag, true);
                }
            } finally {
                cursor.close();
            }
        }
        addTag("", false); //$NON-NLS-1$
    }

    @Override
    public String writeToModel(Task task) {
        // this is a case where we're asked to save but the UI was not yet populated
        if(tagsContainer.getChildCount() == 0)
            return null;

        LinkedHashSet<String> tags = new LinkedHashSet<String>();
        for(int i = 0; i < tagsContainer.getChildCount(); i++) {
            TextView tagName = (TextView)tagsContainer.getChildAt(i).findViewById(R.id.text1);
            if(tagName.getText().length() == 0)
                continue;

            tags.add(tagName.getText().toString());
        }

        if(TagService.getInstance().synchronizeTags(task.getId(), tags)) {
            Flags.set(Flags.TAGS_CHANGED);
            task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        }

        return null;
    }

    /** Adds a tag to the tag field */
    boolean addTag(String tagName, boolean reuse) {
        LayoutInflater inflater = activity.getLayoutInflater();

        // check if already exists
        TextView lastText = null;
        for(int i = 0; i < tagsContainer.getChildCount(); i++) {
            View view = tagsContainer.getChildAt(i);
            lastText = (TextView) view.findViewById(R.id.text1);
            if(lastText.getText().equals(tagName))
                return false;
        }

        final View tagItem;
        if(reuse && lastText != null && lastText.getText().length() == 0) {
            tagItem = (View) lastText.getParent();
        } else {
            tagItem = inflater.inflate(R.layout.tag_edit_row, null);
            tagsContainer.addView(tagItem);
        }

        final AutoCompleteTextView textView = (AutoCompleteTextView)tagItem.
            findViewById(R.id.text1);
        textView.setText(tagName);
        ArrayAdapter<Tag> tagsAdapter =
            new ArrayAdapter<Tag>(activity,
                    android.R.layout.simple_dropdown_item_1line, allTags);
        textView.setAdapter(tagsAdapter);

        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                //
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                //
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                if(count > 0 && tagsContainer.getChildAt(tagsContainer.getChildCount()-1) ==
                        tagItem)
                    addTag("", false); //$NON-NLS-1$
            }
        });

        textView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent arg2) {
                if(actionId != EditorInfo.IME_NULL)
                    return false;
                if(getLastTextView().getText().length() != 0) {
                    addTag("", false); //$NON-NLS-1$
                }
                return true;
            }
        });

        ImageButton reminderRemoveButton;
        reminderRemoveButton = (ImageButton)tagItem.findViewById(R.id.button1);
        reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TextView lastView = getLastTextView();
                if(lastView == textView && textView.getText().length() == 0)
                    return;

                if(tagsContainer.getChildCount() > 1)
                    tagsContainer.removeView(tagItem);
                else
                    textView.setText(""); //$NON-NLS-1$
            }
        });

        return true;
    }

    /**
     * Get tags container last text view. might be null
     * @return
     */
    private TextView getLastTextView() {
        if(tagsContainer.getChildCount() == 0)
            return null;
        View lastItem = tagsContainer.getChildAt(tagsContainer.getChildCount()-1);
        TextView lastText = (TextView) lastItem.findViewById(R.id.text1);
        return lastText;
    }
}