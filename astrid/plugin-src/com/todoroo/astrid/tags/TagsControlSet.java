/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

import android.app.Activity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.tags.TagService.Tag;
import com.todoroo.astrid.ui.PopupControlSet;
import com.todoroo.astrid.utility.Flags;

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TagsControlSet extends PopupControlSet {

    // --- instance variables

    private static final String TRANSITORY_TAGS = "tags";//$NON-NLS-1$

    private final TagService tagService = TagService.getInstance();
    private ArrayList<String> allTagNames;

    private LinearLayout newTags;
    private ListView selectedTags;
    private boolean populated = false;
    private HashMap<String, Integer> tagIndices;
    private final ImageView image;

    //private final LinearLayout tagsContainer;
    private final TextView tagsDisplay;

    public TagsControlSet(Activity activity, int viewLayout, int displayViewLayout, int title) {
        super(activity, viewLayout, displayViewLayout, title);
        DependencyInjectionService.getInstance().inject(this);
        tagsDisplay = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
        image = (ImageView) getDisplayView().findViewById(R.id.display_row_icon);
    }

    private Tag[] getTagArray() {
        ArrayList<Tag> tagsList = TagService.getInstance().getTagList();
        return tagsList.toArray(new Tag[tagsList.size()]);
    }

    private HashMap<String, Integer> buildTagIndices(ArrayList<String> tagNames) {
        HashMap<String, Integer> indices = new HashMap<String, Integer>();
        for (int i = 0; i < tagNames.size(); i++) {
            indices.put(tagNames.get(i), i);
        }
        return indices;
    }

    private ArrayList<String> getTagNames(Tag[] tags) {
        ArrayList<String> names = new ArrayList<String>();
        for (int i = 0; i < tags.length; i++) {
            names.add(tags[i].toString());
        }
        return names;
    }

    private String buildTagString() {
        StringBuilder builder = new StringBuilder();

        LinkedHashSet<String> tags = getTagSet();
        for (String tag : tags) {
            if (builder.length() != 0)
                builder.append(", "); //$NON-NLS-1$
            builder.append(tag);
        }

        return builder.toString();
    }


    private void setTagSelected(String tag) {
        Integer index = tagIndices.get(tag);
        if (index != null) {
            selectedTags.setItemChecked(index, true);
        } else {
            allTagNames.add(tag);
            tagIndices.put(tag, allTagNames.size() - 1);
            ((ArrayAdapter<String>)selectedTags.getAdapter()).notifyDataSetChanged();
        }
    }

    private LinkedHashSet<String> getTagSet() {
        LinkedHashSet<String> tags = new LinkedHashSet<String>();
        if (initialized) {
            for(int i = 0; i < selectedTags.getAdapter().getCount(); i++) {
                if (selectedTags.isItemChecked(i))
                    tags.add(allTagNames.get(i));
            }

            for(int i = 0; i < newTags.getChildCount(); i++) {
                TextView tagName = (TextView) newTags.getChildAt(i).findViewById(R.id.text1);
                if(tagName.getText().length() == 0)
                    continue;

                tags.add(tagName.getText().toString());
            }
        } else {
            if (model.getTransitory(TRANSITORY_TAGS) != null) {
                return (LinkedHashSet<String>) model.getTransitory(TRANSITORY_TAGS);
            }
        }
        return tags;
    }

    /** Adds a tag to the tag field */
    boolean addTag(String tagName, boolean reuse) {
        LayoutInflater inflater = activity.getLayoutInflater();

        // check if already exists
        TextView lastText = null;
        for(int i = 0; i < newTags.getChildCount(); i++) {
            View view = newTags.getChildAt(i);
            lastText = (TextView) view.findViewById(R.id.text1);
            if(lastText.getText().equals(tagName))
                return false;
        }

        final View tagItem;
        if(reuse && lastText != null && lastText.getText().length() == 0) {
            tagItem = (View) lastText.getParent();
        } else {
            tagItem = inflater.inflate(R.layout.tag_edit_row, null);
            newTags.addView(tagItem);
        }
        if(tagName == null)
            tagName = ""; //$NON-NLS-1$

        final AutoCompleteTextView textView = (AutoCompleteTextView)tagItem.
            findViewById(R.id.text1);
        textView.setText(tagName);

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
                if(count > 0 && newTags.getChildAt(newTags.getChildCount()-1) ==
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

                if(newTags.getChildCount() > 1)
                    newTags.removeView(tagItem);
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
        if(newTags.getChildCount() == 0)
            return null;
        View lastItem = newTags.getChildAt(newTags.getChildCount()-1);
        TextView lastText = (TextView) lastItem.findViewById(R.id.text1);
        return lastText;
    }

    @Override
    public void readFromTask(Task task) {
        super.readFromTask(task);
        if(model.getId() != AbstractModel.NO_ID) {
            TodorooCursor<Metadata> cursor = tagService.getTags(model.getId());
            LinkedHashSet<String> tags = new LinkedHashSet<String>(cursor.getCount());
            try {
                for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    String tag = cursor.get(TaskToTagMetadata.TAG_NAME);
                    tags.add(tag);
                }
            } finally {
                cursor.close();
            }
            model.putTransitory(TRANSITORY_TAGS, tags);
            refreshDisplayView();
        }
    }

    @Override
    protected void readFromTaskOnInitialize() {
        newTags.removeAllViews();

        for (int i = 0; i < selectedTags.getCount(); i++) { // clear all selected items
            selectedTags.setItemChecked(i, false);
        }
        if(model.getId() != AbstractModel.NO_ID) {
            selectTagsFromModel();
        }
        addTag("", false); //$NON-NLS-1$
        refreshDisplayView();
        populated = true;
    }

    private void selectTagsFromModel() {
        LinkedHashSet<String> tags = (LinkedHashSet<String>) model.getTransitory(TRANSITORY_TAGS);
        if (tags != null) {
            for (String tag : tags) {
                setTagSelected(tag);
            }
        }
    }

    @Override
    protected void additionalDialogSetup() {
        super.additionalDialogSetup();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    protected void afterInflate() {
        Tag[] allTags = getTagArray();
        allTagNames = getTagNames(allTags);
        tagIndices = buildTagIndices(allTagNames);

        selectedTags = (ListView) getView().findViewById(R.id.existingTags);
        selectedTags.setAdapter(new ArrayAdapter<String>(activity,
                R.layout.simple_list_item_multiple_choice_themed, allTagNames));
        selectedTags.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        this.newTags = (LinearLayout) getView().findViewById(R.id.newTags);
    }

    @Override
    protected String writeToModelAfterInitialized(Task task) {
        // this is a case where we're asked to save but the UI was not yet populated
        if(!populated)
            return null;

        LinkedHashSet<String> tags = getTagSet();

        if(TagService.getInstance().synchronizeTags(task.getId(), task.getValue(Task.UUID), tags)) {
            Flags.set(Flags.TAGS_CHANGED);
            task.setValue(Task.MODIFICATION_DATE, DateUtilities.now());
        }

        return null;
    }

    @Override
    protected void refreshDisplayView() {
        String tagString = buildTagString();
        if (!TextUtils.isEmpty(tagString)) {
            tagsDisplay.setText(tagString);
            tagsDisplay.setTextColor(themeColor);
            image.setImageResource(ThemeService.getTaskEditDrawable(R.drawable.tea_icn_lists, R.drawable.tea_icn_lists_lightblue));
        } else {
            tagsDisplay.setText(R.string.tag_FEx_untagged);
            tagsDisplay.setTextColor(unsetColor);
            image.setImageResource(R.drawable.tea_icn_lists_gray);
        }
    }

    public boolean hasLists() {
        LinkedHashSet<String> tags = getTagSet();
        return !tags.isEmpty();
    }

}
