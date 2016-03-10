/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.FragmentComponent;
import org.tasks.ui.TaskEditControlFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.filter;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TagsControlSet extends TaskEditControlFragment {

    public static final int TAG = R.string.TEA_ctrl_lists_pref;

    private static final String EXTRA_TAGS = "extra_tags";

    @Inject MetadataDao metadataDao;
    @Inject TagDataDao tagDataDao;
    @Inject TagService tagService;
    @Inject DialogBuilder dialogBuilder;

    @Bind(R.id.display_row_edit) TextView tagsDisplay;

    private long taskId;
    private ArrayList<String> allTagNames;
    private LinearLayout newTags;
    private ListView selectedTags;
    private View dialogView;
    private AlertDialog dialog;
    private ArrayList<String> tagList;

    private String buildTagString() {
        StringBuilder builder = new StringBuilder();

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null) {
            tagList = savedInstanceState.getStringArrayList(EXTRA_TAGS);
        } else {
            tagList = tagService.getTagNames(taskId);
        }
        allTagNames = newArrayList(ImmutableSet.copyOf(transform(tagService.getTagList(), new Function<TagData, String>() {
            @Override
            public String apply(TagData tagData) {
                return tagData.getName();
            }
        })));
        dialogView = inflater.inflate(R.layout.control_set_tag_list, null);
        newTags = (LinearLayout) dialogView.findViewById(R.id.newTags);
        selectedTags = (ListView) dialogView.findViewById(R.id.existingTags);
        selectedTags.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.simple_list_item_multiple_choice_themed, allTagNames));
        addTag("");
        for (String tag : tagList) {
            setTagSelected(tag);
        }
        refreshDisplayView();
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList(EXTRA_TAGS, tagList);
    }

    @Override
    protected int getLayout() {
        return R.layout.control_set_tags;
    }

    @Override
    public void initialize(boolean isNewTask, Task task) {
        taskId = task.getId();
    }

    @Override
    public void apply(Task task) {
        if (synchronizeTags(task.getId(), task.getUUID())) {
            Flags.set(Flags.TAGS_CHANGED);
            task.setModificationDate(DateUtilities.now());
        }
    }

    @OnClick(R.id.display_row_edit)
    void openPopup(View view) {
        if (dialog == null) {
            buildDialog();
        }
        dialog.show();
    }

    protected Dialog buildDialog() {
        android.support.v7.app.AlertDialog.Builder builder = dialogBuilder.newDialog()
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tagList = getTagList();
                        refreshDisplayView();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        refreshDisplayView();
                    }
                });
        dialog = builder.show();
        return dialog;
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

    private ArrayList<String> getTagList() {
        Set<String> tags = new LinkedHashSet<>();
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
        return newArrayList(tags);
    }

    /** Adds a tag to the tag field */
    void addTag(String tagName) {
        LayoutInflater inflater = getActivity().getLayoutInflater();

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
    public int getIcon() {
        return R.drawable.ic_label_24dp;
    }

    @Override
    public int controlId() {
        return TAG;
    }

    @Override
    public boolean hasChanges(Task original) {
        return !getExistingTags(original.getUUID()).equals(getSelectedTags(false));
    }

    protected void refreshDisplayView() {
        String tagString = buildTagString();
        if (!TextUtils.isEmpty(tagString)) {
            tagsDisplay.setText(tagString);
            tagsDisplay.setAlpha(1.0f);
        } else {
            tagsDisplay.setText(R.string.tag_FEx_untagged);
            tagsDisplay.setAlpha(0.5f);
        }
    }

    private Set<TagData> getSelectedTags(final boolean createMissingTags) {
        return newHashSet(transform(tagList, new Function<String, TagData>() {
            @Override
            public TagData apply(String tagName) {
                TagData tagData = tagDataDao.getTagByName(tagName, TagData.PROPERTIES);
                if (tagData == null) {
                    // create missing tags
                    tagData = new TagData();
                    tagData.setName(tagName);
                    if (createMissingTags) {
                        tagDataDao.persist(tagData);
                    }
                }
                return tagData;
            }
        }));
    }

    private Set<TagData> getExistingTags(String taskUuid) {
        Query query = Query.select(Metadata.PROPERTIES).where(
                Criterion.and(
                        TaskToTagMetadata.TASK_UUID.eq(taskUuid),
                        Metadata.DELETION_DATE.eq(0))
        );
        return newHashSet(transform(metadataDao.toList(query), new Function<Metadata, TagData>() {
            @Override
            public TagData apply(Metadata metadata) {
                return tagDataDao.getByUuid(metadata.getValue(TaskToTagMetadata.TAG_UUID));
            }
        }));
    }

    /**
     * Save the given array of tags into the database
     */
    private boolean synchronizeTags(long taskId, String taskUuid) {
        Set<TagData> existingTags = getExistingTags(taskUuid);
        Set<TagData> selectedTags = getSelectedTags(true);
        Sets.SetView<TagData> added = difference(selectedTags, existingTags);
        Sets.SetView<TagData> removed = difference(existingTags, selectedTags);
        deleteLinks(taskId, taskUuid, filter(removed, notNull()));
        for (TagData tagData : added) {
            Metadata newLink = TaskToTagMetadata.newTagMetadata(taskId, taskUuid, tagData.getName(), tagData.getUuid());
            metadataDao.createNew(newLink);
        }
        return !removed.isEmpty() || !added.isEmpty();
    }

    /**
     * Delete all links between the specified task and the list of tags
     */
    private void deleteLinks(long taskId, String taskUuid, Iterable<TagData> tags) {
        Metadata deleteTemplate = new Metadata();
        deleteTemplate.setTask(taskId); // Need this for recording changes in outstanding table
        deleteTemplate.setDeletionDate(DateUtilities.now());
        for (TagData tag : tags) {
            // TODO: Right now this is in a loop because each deleteTemplate needs the individual tagUuid in order to record
            // the outstanding entry correctly. If possible, this should be improved to a single query
            deleteTemplate.setValue(TaskToTagMetadata.TAG_UUID, tag.getUuid()); // Need this for recording changes in outstanding table
            metadataDao.update(Criterion.and(MetadataDao.MetadataCriteria.withKey(TaskToTagMetadata.KEY), Metadata.DELETION_DATE.eq(0),
                    TaskToTagMetadata.TASK_UUID.eq(taskUuid), TaskToTagMetadata.TAG_UUID.eq(tag.getUuid())), deleteTemplate);
        }
    }

    @Override
    protected void inject(FragmentComponent component) {
        component.inject(this);
    }
}
