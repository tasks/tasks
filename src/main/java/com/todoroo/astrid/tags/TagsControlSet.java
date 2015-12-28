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
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.google.common.base.Strings;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.todoroo.andlib.data.AbstractModel;
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
import org.tasks.dialogs.DialogBuilder;
import org.tasks.preferences.ActivityPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;

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

    //private final LinearLayout tagsContainer;
    private final TextView tagsDisplay;

    private final MetadataDao metadataDao;
    private final TagDataDao tagDataDao;
    private final TagService tagService;

    public TagsControlSet(MetadataDao metadataDao, TagDataDao tagDataDao, ActivityPreferences preferences, TagService tagService, Activity activity, DialogBuilder dialogBuilder) {
        super(preferences, activity, R.layout.control_set_tag_list, R.layout.control_set_tags, R.string.TEA_tags_label_long, dialogBuilder);
        this.metadataDao = metadataDao;
        this.tagDataDao = tagDataDao;
        this.tagService = tagService;
        tagsDisplay = (TextView) getView().findViewById(R.id.display_row_edit);
    }

    private String buildTagString() {
        StringBuilder builder = new StringBuilder();

        List<String> tagList = getTagList();
        Collections.sort(tagList);
        for (String tag : tagList) {
            if (tag.trim().length() == 0) {
                continue;
            }
            if (builder.length() != 0) {
                builder.append(", "); //$NON-NLS-1$
            }
            builder.append(tag);
        }

        return builder.toString();
    }

    private void setTagSelected(String tag) {
        int index = allTagNames.indexOf(tag);
        if (index >= 0) {
            selectedTags.setItemChecked(index, true);
        } else {
            allTagNames.add(tag);
            ((ArrayAdapter<String>)selectedTags.getAdapter()).notifyDataSetChanged();
        }
    }

    private List<String> getTagList() {
        Set<String> tags = new LinkedHashSet<>();
        if (initialized) {
            for(int i = 0; i < selectedTags.getAdapter().getCount(); i++) {
                if (selectedTags.isItemChecked(i)) {
                    tags.add(allTagNames.get(i));
                }
            }
            for (int i = newTags.getChildCount() - 1 ; i >= 0 ; i--) {
                TextView tagName = (TextView) newTags.getChildAt(i).findViewById(R.id.text1);
                final String text = tagName.getText().toString();
                if (Strings.isNullOrEmpty(text)) {
                    continue;
                }
                TagData tagByName = tagDataDao.getTagByName(text, TagData.PROPERTIES);
                if (tagByName != null) {
                    setTagSelected(tagByName.getName());
                    tags.add(tagByName.getName());
                    newTags.removeViewAt(i);
                } else if (!Iterables.any(tags, new Predicate<String>() {
                    @Override
                    public boolean apply(String input) {
                        return text.equalsIgnoreCase(input);
                    }
                })) {
                    tags.add(text);
                }
            }
        } else {
            if (model.getTransitory(TRANSITORY_TAGS) != null) {
                return (List<String>) model.getTransitory(TRANSITORY_TAGS);
            }
        }
        return newArrayList(tags);
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

        tagItem.findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
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
            model.putTransitory(TRANSITORY_TAGS, tagService.getTagNames(model.getId()));
            refreshDisplayView();
        }
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_label_24dp;
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
        List<String> tags = (List<String>) model.getTransitory(TRANSITORY_TAGS);
        if (tags != null) {
            for (String tag : tags) {
                setTagSelected(tag);
            }
        }
    }

    @Override
    protected void afterInflate() {
        allTagNames = newArrayList(ImmutableSet.copyOf(transform(tagService.getTagList(), new Function<TagData, String>() {
            @Override
            public String apply(TagData tagData) {
                return tagData.getName();
            }
        })));

        selectedTags = (ListView) getDialogView().findViewById(R.id.existingTags);
        selectedTags.setAdapter(new ArrayAdapter<>(activity,
                R.layout.simple_list_item_multiple_choice_themed, allTagNames));
        selectedTags.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        newTags = (LinearLayout) getDialogView().findViewById(R.id.newTags);
    }

    @Override
    protected void writeToModelAfterInitialized(Task task) {
        // this is a case where we're asked to save but the UI was not yet populated
        if(!populated) {
            return;
        }

        synchronizeTags(task.getId(), task.getUUID());
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
    private void synchronizeTags(long taskId, String taskUuid) {
        Query query = Query.select(Metadata.PROPERTIES).where(
                Criterion.and(
                        TaskToTagMetadata.TASK_UUID.eq(taskUuid),
                        Metadata.DELETION_DATE.eq(0))
        );
        Set<String> existingTagIds = newHashSet(transform(metadataDao.toList(query), new Function<Metadata, String>() {
            @Override
            public String apply(Metadata tagData) {
                return tagData.getValue(TaskToTagMetadata.TAG_UUID);
            }
        }));
        List<String> tags = getTagList();
        // create missing tags
        for (String tag : tags) {
            TagData tagData = tagDataDao.getTagByName(tag, TagData.ID);
            if (tagData == null) {
                tagData = new TagData();
                tagData.setName(tag);
                tagDataDao.persist(tagData);
            }
        }
        List<TagData> selectedTags = newArrayList(transform(tags, new Function<String, TagData>() {
            @Override
            public TagData apply(String tag) {
                return tagDataDao.getTagByName(tag, TagData.PROPERTIES);
            }
        }));
        deleteLinks(taskId, taskUuid, difference(existingTagIds, newHashSet(Iterables.transform(selectedTags, new Function<TagData, String>() {
            @Override
            public String apply(TagData tagData) {
                return tagData.getUUID();
            }
        }))));
        for (TagData tagData : selectedTags) {
            if (!existingTagIds.contains(tagData.getUUID())) {
                Metadata newLink = TaskToTagMetadata.newTagMetadata(taskId, taskUuid, tagData.getName(), tagData.getUUID());
                metadataDao.createNew(newLink);
            }
        }
    }

    /**
     * Delete all links between the specified task and the list of tags
     */
    private void deleteLinks(long taskId, String taskUuid, Iterable<String> tagUuids) {
        Metadata deleteTemplate = new Metadata();
        deleteTemplate.setTask(taskId); // Need this for recording changes in outstanding table
        deleteTemplate.setDeletionDate(DateUtilities.now());
        for (String uuid : tagUuids) {
            // TODO: Right now this is in a loop because each deleteTemplate needs the individual tagUuid in order to record
            // the outstanding entry correctly. If possible, this should be improved to a single query
            deleteTemplate.setValue(TaskToTagMetadata.TAG_UUID, uuid); // Need this for recording changes in outstanding table
            metadataDao.update(Criterion.and(MetadataDao.MetadataCriteria.withKey(TaskToTagMetadata.KEY), Metadata.DELETION_DATE.eq(0),
                    TaskToTagMetadata.TASK_UUID.eq(taskUuid), TaskToTagMetadata.TAG_UUID.eq(uuid)), deleteTemplate);
        }
    }
}
