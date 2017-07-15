/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.tags;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.RemoteModel;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.utility.Flags;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.FragmentComponent;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;
import org.tasks.ui.TaskEditControlFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.filter;
import static com.google.common.collect.Sets.newHashSet;
import static com.todoroo.andlib.utility.AndroidUtilities.atLeastJellybeanMR1;

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public final class TagsControlSet extends TaskEditControlFragment {

    public static final int TAG = R.string.TEA_ctrl_lists_pref;

    private static final char SPACE = '\u0020';
    private static final char NO_BREAK_SPACE = '\u00a0';
    private static final String EXTRA_NEW_TAGS = "extra_new_tags";
    private static final String EXTRA_SELECTED_TAGS = "extra_selected_tags";

    @Inject MetadataDao metadataDao;
    @Inject TagDataDao tagDataDao;
    @Inject TagService tagService;
    @Inject DialogBuilder dialogBuilder;
    @Inject ThemeCache themeCache;

    @BindView(R.id.display_row_edit) TextView tagsDisplay;

    private long taskId;
    private LinearLayout newTagLayout;
    private ListView tagListView;
    private View dialogView;
    private AlertDialog dialog;
    private List<TagData> allTags;
    private ArrayList<TagData> selectedTags;

    private final Ordering<TagData> orderByName = new Ordering<TagData>() {
        @Override
        public int compare(TagData left, TagData right) {
            return left.getName().compareTo(right.getName());
        }
    };

    private Function<TagData, SpannableString> tagToString(final float maxLength) {
        return tagData -> {
            String tagName = tagData.getName();
            tagName = tagName
                    .substring(0, Math.min(tagName.length(), (int) maxLength))
                    .replace(' ', NO_BREAK_SPACE);
            SpannableString string = new SpannableString(NO_BREAK_SPACE + tagName + NO_BREAK_SPACE);
            int themeIndex = tagData.getColor();
            ThemeColor color = themeIndex >= 0 ? themeCache.getThemeColor(themeIndex) : themeCache.getUntaggedColor();
            string.setSpan(new BackgroundColorSpan(color.getPrimaryColor()), 0, string.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            string.setSpan(new ForegroundColorSpan(color.getActionBarTint()), 0, string.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            return string;
        };
    }

    private CharSequence buildTagString() {
        List<TagData> sortedTagData = orderByName.sortedCopy(selectedTags);
        List<SpannableString> tagStrings = Lists.transform(sortedTagData, tagToString(Float.MAX_VALUE));
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (SpannableString tagString : tagStrings) {
            if (builder.length() > 0) {
                builder.append(SPACE);
            }
            builder.append(tagString);
        }
        return builder;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ArrayList<String> newTags;
        if (savedInstanceState != null) {
            selectedTags = savedInstanceState.getParcelableArrayList(EXTRA_SELECTED_TAGS);
            newTags = savedInstanceState.getStringArrayList(EXTRA_NEW_TAGS);
        } else {
            selectedTags = tagService.getTagDataForTask(taskId);
            newTags = newArrayList();
        }
        allTags = tagService.getTagList();
        dialogView = inflater.inflate(R.layout.control_set_tag_list, null);
        newTagLayout = (LinearLayout) dialogView.findViewById(R.id.newTags);
        tagListView = (ListView) dialogView.findViewById(R.id.existingTags);
        tagListView.setAdapter(new ArrayAdapter<TagData>(getActivity(), R.layout.simple_list_item_multiple_choice_themed, allTags) {
            @NonNull
            @SuppressLint("NewApi")
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
                TagData tagData = allTags.get(position);
                ThemeColor themeColor = themeCache.getThemeColor(tagData.getColor() >= 0 ? tagData.getColor() : 19);
                view.setText(tagData.getName());
                Drawable original = ContextCompat.getDrawable(getContext(), R.drawable.ic_label_24dp);
                Drawable wrapped = DrawableCompat.wrap(original.mutate());
                DrawableCompat.setTint(wrapped, themeColor.getPrimaryColor());
                if (atLeastJellybeanMR1()) {
                    view.setCompoundDrawablesRelativeWithIntrinsicBounds(wrapped, null, null, null);
                } else {
                    view.setCompoundDrawablesWithIntrinsicBounds(wrapped, null, null, null);
                }
                return view;
            }
        });
        for (String newTag : newTags) {
            addTag(newTag);
        }
        addTag("");
        for (TagData tag : selectedTags) {
            setTagSelected(tag);
        }
        refreshDisplayView();
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(EXTRA_SELECTED_TAGS, selectedTags);
        outState.putStringArrayList(EXTRA_NEW_TAGS, getNewTags());
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
            dialog = buildDialog();
        }
        dialog.show();
    }

    private AlertDialog buildDialog() {
        return dialogBuilder.newDialog()
                .setView(dialogView)
                .setOnDismissListener(dialogInterface -> refreshDisplayView())
                .create();
    }

    private void setTagSelected(TagData tag) {
        int index = allTags.indexOf(tag);
        if (index >= 0) {
            tagListView.setItemChecked(index, true);
        }
    }

    private boolean isSelected(List<TagData> selected, final String name) {
        return Iterables.any(selected, input -> name.equalsIgnoreCase(input.getName()));
    }

    private ArrayList<TagData> getSelectedTags() {
        ArrayList<TagData> tags = new ArrayList<>();
        for(int i = 0; i < tagListView.getAdapter().getCount(); i++) {
            if (tagListView.isItemChecked(i)) {
                tags.add(allTags.get(i));
            }
        }
        for (int i = newTagLayout.getChildCount() - 1; i >= 0 ; i--) {
            TextView tagName = (TextView) newTagLayout.getChildAt(i).findViewById(R.id.text1);
            final String text = tagName.getText().toString();
            if (Strings.isNullOrEmpty(text)) {
                continue;
            }
            TagData tagByName = tagDataDao.getTagByName(text, TagData.PROPERTIES);
            if (tagByName != null) {
                if (!isSelected(tags, text)) {
                    setTagSelected(tagByName);
                    tags.add(tagByName);
                }
                newTagLayout.removeViewAt(i);
            } else if (!isSelected(tags, text)) {
                TagData newTag = new TagData();
                newTag.setName(text);
                tags.add(newTag);
            }
        }
        return tags;
    }

    private ArrayList<String> getNewTags() {
        ArrayList<String> tags = new ArrayList<>();
        for (int i = newTagLayout.getChildCount() - 1 ; i >= 0 ; i--) {
            TextView textView = (TextView) newTagLayout.getChildAt(i).findViewById(R.id.text1);
            String tagName = textView.getText().toString();
            if (Strings.isNullOrEmpty(tagName)) {
                continue;
            }
            tags.add(tagName);
        }
        return tags;
    }

    /** Adds a tag to the tag field */
    private void addTag(String tagName) {
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // check if already exists
        TextView lastText;
        for(int i = 0; i < newTagLayout.getChildCount(); i++) {
            View view = newTagLayout.getChildAt(i);
            lastText = (TextView) view.findViewById(R.id.text1);
            if(lastText.getText().equals(tagName)) {
                return;
            }
        }

        final View tagItem;
        tagItem = inflater.inflate(R.layout.tag_edit_row, null);
        newTagLayout.addView(tagItem);
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
                if(count > 0 && newTagLayout.getChildAt(newTagLayout.getChildCount()-1) ==
                        tagItem) {
                    addTag(""); //$NON-NLS-1$
                }
            }
        });

        textView.setOnEditorActionListener((arg0, actionId, arg2) -> {
            if(actionId != EditorInfo.IME_NULL) {
                return false;
            }
            if(getLastTextView().getText().length() != 0) {
                addTag(""); //$NON-NLS-1$
            }
            return true;
        });

        tagItem.findViewById(R.id.button1).setOnClickListener(v -> {
            TextView lastView = getLastTextView();
            if(lastView == textView && textView.getText().length() == 0) {
                return;
            }

            if(newTagLayout.getChildCount() > 1) {
                newTagLayout.removeView(tagItem);
            } else {
                textView.setText(""); //$NON-NLS-1$
            }
        });
    }

    /**
     * Get tags container last text view. might be null
     */
    private TextView getLastTextView() {
        if(newTagLayout.getChildCount() == 0) {
            return null;
        }
        View lastItem = newTagLayout.getChildAt(newTagLayout.getChildCount()-1);
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
        Set<TagData> existingSet = newHashSet(tagService.getTagDataForTask(original.getUUID()));
        Set<TagData> selectedSet = newHashSet(selectedTags);
        return !existingSet.equals(selectedSet);
    }

    private void refreshDisplayView() {
        selectedTags = getSelectedTags();
        CharSequence tagString = buildTagString();
        if (TextUtils.isEmpty(tagString)) {
            tagsDisplay.setText(R.string.tag_FEx_untagged);
        } else {
            tagsDisplay.setText(tagString);
        }
    }

    /**
     * Save the given array of tags into the database
     */
    private boolean synchronizeTags(long taskId, String taskUuid) {
        for (TagData tagData : selectedTags) {
            if (RemoteModel.NO_UUID.equals(tagData.getUuid())) {
                tagDataDao.persist(tagData);
            }
        }
        Set<TagData> existingHash = newHashSet(tagService.getTagDataForTask(taskUuid));
        Set<TagData> selectedHash = newHashSet(selectedTags);
        Sets.SetView<TagData> added = difference(selectedHash, existingHash);
        Sets.SetView<TagData> removed = difference(existingHash, selectedHash);
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
