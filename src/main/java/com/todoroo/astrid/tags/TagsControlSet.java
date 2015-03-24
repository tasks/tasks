/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.todoroo.andlib.data.AbstractModel;
import com.todoroo.andlib.data.Callback;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.ui.PopupControlSet;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;
import org.tasks.preferences.ActivityPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TagsControlSet extends PopupControlSet {

    // --- instance variables

    private static final String TRANSITORY_TAGS = "tags";//$NON-NLS-1$

    private ArrayList<String> allTagNames;

    private LinearLayout newTags;
    private ListView selectedTags;
    private boolean populated = false;
    private HashMap<String, Integer> tagIndices;

    //private final LinearLayout tagsContainer;
    private final TextView tagsDisplay;

    private final MetadataDao metadataDao;
    private final TagDataDao tagDataDao;
    private final TagService tagService;

    public TagsControlSet(MetadataDao metadataDao, TagDataDao tagDataDao, ActivityPreferences preferences, TagService tagService, Activity activity) {
        super(preferences, activity, R.layout.control_set_tag_list, R.layout.control_set_tags, R.string.TEA_tags_label_long);
        this.metadataDao = metadataDao;
        this.tagDataDao = tagDataDao;
        this.tagService = tagService;
        tagsDisplay = (TextView) getDisplayView().findViewById(R.id.display_row_edit);
    }

    private TagData[] getTagArray() {
        List<TagData> tagList = tagService.getTagList();
        return tagList.toArray(new TagData[tagList.size()]);
    }

    private HashMap<String, Integer> buildTagIndices(ArrayList<String> tagNames) {
        HashMap<String, Integer> indices = new HashMap<>();
        for (int i = 0; i < tagNames.size(); i++) {
            indices.put(tagNames.get(i), i);
        }
        return indices;
    }

    private ArrayList<String> getTagNames(TagData[] tags) {
        ArrayList<String> names = new ArrayList<>();
        for (TagData tag : tags) {
            if (!names.contains(tag.getName())) {
                names.add(tag.getName());
            }
        }
        return names;
    }

    private String buildTagString() {
        StringBuilder builder = new StringBuilder();

        LinkedHashSet<String> tags = getTagSet();
        for (String tag : tags) {
            if (builder.length() != 0) {
                builder.append(", "); //$NON-NLS-1$
            }
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
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (initialized) {
            for(int i = 0; i < selectedTags.getAdapter().getCount(); i++) {
                if (selectedTags.isItemChecked(i)) {
                    tags.add(allTagNames.get(i));
                }
            }

            for(int i = 0; i < newTags.getChildCount(); i++) {
                TextView tagName = (TextView) newTags.getChildAt(i).findViewById(R.id.text1);
                if(tagName.getText().length() == 0) {
                    continue;
                }

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
    void addTag(String tagName) {
        LayoutInflater inflater = activity.getLayoutInflater();

        // check if already exists
        TextView lastText;
        for(int i = 0; i < newTags.getChildCount(); i++) {
            View view = newTags.getChildAt(i);
            lastText = (TextView) view.findViewById(R.id.text1);
            if(lastText.getText().equals(tagName)) {
                return;
            }
        }

        final View tagItem;
        tagItem = inflater.inflate(R.layout.tag_edit_row, null);
        newTags.addView(tagItem);
        if(tagName == null) {
            tagName = ""; //$NON-NLS-1$
        }

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
                        tagItem) {
                    addTag(""); //$NON-NLS-1$
                }
            }
        });

        textView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent arg2) {
                if(actionId != EditorInfo.IME_NULL) {
                    return false;
                }
                if(getLastTextView().getText().length() != 0) {
                    addTag(""); //$NON-NLS-1$
                }
                return true;
            }
        });

        ImageButton reminderRemoveButton;
        reminderRemoveButton = (ImageButton)tagItem.findViewById(R.id.button1);
        reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView lastView = getLastTextView();
                if(lastView == textView && textView.getText().length() == 0) {
                    return;
                }

                if(newTags.getChildCount() > 1) {
                    newTags.removeView(tagItem);
                } else {
                    textView.setText(""); //$NON-NLS-1$
                }
            }
        });
    }

    /**
     * Get tags container last text view. might be null
     */
    private TextView getLastTextView() {
        if(newTags.getChildCount() == 0) {
            return null;
        }
        View lastItem = newTags.getChildAt(newTags.getChildCount()-1);
        return (TextView) lastItem.findViewById(R.id.text1);
    }

    @Override
    public void readFromTask(Task task) {
        super.readFromTask(task);
        if(model.getId() != AbstractModel.NO_ID) {
            model.putTransitory(TRANSITORY_TAGS, new LinkedHashSet<>(tagService.getTagNames(model.getId())));
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
        addTag(""); //$NON-NLS-1$
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
        TagData[] allTags = getTagArray();
        allTagNames = getTagNames(allTags);
        tagIndices = buildTagIndices(allTagNames);

        selectedTags = (ListView) getView().findViewById(R.id.existingTags);
        selectedTags.setAdapter(new ArrayAdapter<>(activity,
                R.layout.simple_list_item_multiple_choice_themed, allTagNames));
        selectedTags.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        this.newTags = (LinearLayout) getView().findViewById(R.id.newTags);
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        // this is a case where we're asked to save but the UI was not yet populated
        if(!populated) {
            return;
        }

        LinkedHashSet<String> tags = getTagSet();

        synchronizeTags(task.getId(), task.getUUID(), tags);
        Flags.set(Flags.TAGS_CHANGED);
        task.setModificationDate(DateUtilities.now());
    }

    @Override
    protected void refreshDisplayView() {
        String tagString = buildTagString();
        if (!TextUtils.isEmpty(tagString)) {
            tagsDisplay.setText(tagString);
            tagsDisplay.setTextColor(themeColor);
        } else {
            tagsDisplay.setText(R.string.tag_FEx_untagged);
            tagsDisplay.setTextColor(unsetColor);
        }
    }

    /**
     * Save the given array of tags into the database
     */
    private void synchronizeTags(long taskId, String taskUuid, Set<String> tags) {
        Query query = Query.select(Metadata.PROPERTIES).where(
                Criterion.and(
                        TaskToTagMetadata.TASK_UUID.eq(taskUuid),
                        Metadata.DELETION_DATE.eq(0))
        );
        final HashSet<String> existingLinks = new HashSet<>();
        metadataDao.query(query, new Callback<Metadata>() {
            @Override
            public void apply(Metadata link) {
                existingLinks.add(link.getValue(TaskToTagMetadata.TAG_UUID));
            }
        });

        for (String tag : tags) {
            TagData tagData = tagDataDao.getTagByName(tag, TagData.NAME, TagData.UUID);
            if (tagData == null) {
                tagData = new TagData();
                tagData.setName(tag);
                tagDataDao.persist(tagData);
            }
            if (existingLinks.contains(tagData.getUUID())) {
                existingLinks.remove(tagData.getUUID());
            } else {
                Metadata newLink = TaskToTagMetadata.newTagMetadata(taskId, taskUuid, tag, tagData.getUUID());
                metadataDao.createNew(newLink);
            }
        }

        // Mark as deleted links that don't exist anymore
        deleteLinks(taskId, taskUuid, existingLinks.toArray(new String[existingLinks.size()]));
    }

    /**
     * Delete all links between the specified task and the list of tags
     */
    private void deleteLinks(long taskId, String taskUuid, String[] tagUuids) {
        Metadata deleteTemplate = new Metadata();
        deleteTemplate.setTask(taskId); // Need this for recording changes in outstanding table
        deleteTemplate.setDeletionDate(DateUtilities.now());
        if (tagUuids != null) {
            for (String uuid : tagUuids) {
                // TODO: Right now this is in a loop because each deleteTemplate needs the individual tagUuid in order to record
                // the outstanding entry correctly. If possible, this should be improved to a single query
                deleteTemplate.setValue(TaskToTagMetadata.TAG_UUID, uuid); // Need this for recording changes in outstanding table
                metadataDao.update(Criterion.and(MetadataDao.MetadataCriteria.withKey(TaskToTagMetadata.KEY), Metadata.DELETION_DATE.eq(0),
                        TaskToTagMetadata.TASK_UUID.eq(taskUuid), TaskToTagMetadata.TAG_UUID.eq(uuid)), deleteTemplate);
            }
        }
    }
}
