/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.tags;

import static android.app.Activity.RESULT_OK;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import butterknife.BindView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.common.base.Predicates;
import com.google.common.collect.Ordering;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.data.Task;
import java.util.ArrayList;
import java.util.Set;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;
import org.tasks.injection.FragmentComponent;
import org.tasks.tags.TagPickerActivity;
import org.tasks.ui.ChipProvider;
import org.tasks.ui.TaskEditControlFragment;

/**
 * Control set to manage adding and removing tags
 *
 * @author Tim Su <tim@todoroo.com>
 */
public final class TagsControlSet extends TaskEditControlFragment {

  public static final int TAG = R.string.TEA_ctrl_lists_pref;

  private static final String EXTRA_ORIGINAL_TAGS = "extra_original_tags";
  private static final String EXTRA_SELECTED_TAGS = "extra_selected_tags";
  private static final int REQUEST_TAG_PICKER_ACTIVITY = 10582;
  private final Ordering<TagData> orderByName =
      new Ordering<TagData>() {
        @Override
        public int compare(TagData left, TagData right) {
          return left.getName().compareTo(right.getName());
        }
      };
  @Inject TagDao tagDao;
  @Inject TagDataDao tagDataDao;
  @Inject ChipProvider chipProvider;

  @BindView(R.id.no_tags)
  TextView tagsDisplay;
  @BindView(R.id.chip_group)
  ChipGroup chipGroup;

  private ArrayList<TagData> originalTags;
  private ArrayList<TagData> selectedTags;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    if (savedInstanceState != null) {
      selectedTags = savedInstanceState.getParcelableArrayList(EXTRA_SELECTED_TAGS);
      originalTags = savedInstanceState.getParcelableArrayList(EXTRA_ORIGINAL_TAGS);
    } else {
      originalTags =
          newArrayList(
              task.isNew()
                  ? from(task.getTags())
                      .transform(tagDataDao::getTagByName)
                      .filter(Predicates.notNull())
                  : tagDataDao.getTagDataForTask(task.getId()));
      selectedTags = new ArrayList<>(originalTags);
    }
    refreshDisplayView();
    return view;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelableArrayList(EXTRA_SELECTED_TAGS, selectedTags);
    outState.putParcelableArrayList(EXTRA_ORIGINAL_TAGS, originalTags);
  }

  @Override
  protected int getLayout() {
    return R.layout.control_set_tags;
  }

  @Override
  public void apply(Task task) {
    if (tagDao.applyTags(task, tagDataDao, selectedTags)) {
      task.setModificationDate(DateUtilities.now());
    }
  }

  @Override
  protected void onRowClick() {
    Intent intent = new Intent(getContext(), TagPickerActivity.class);
    intent.putParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED, selectedTags);
    startActivityForResult(intent, REQUEST_TAG_PICKER_ACTIVITY);
  }

  @Override
  protected boolean isClickable() {
    return true;
  }

  @Override
  public int getIcon() {
    return R.drawable.ic_outline_label_24px;
  }

  @Override
  public int controlId() {
    return TAG;
  }

  @Override
  public boolean hasChanges(Task original) {
    Set<TagData> originalSet = newHashSet(originalTags);
    Set<TagData> selectedSet = newHashSet(selectedTags);
    return !originalSet.equals(selectedSet);
  }

  private void refreshDisplayView() {
    if (selectedTags.isEmpty()) {
      chipGroup.setVisibility(View.GONE);
      tagsDisplay.setVisibility(View.VISIBLE);
    } else {
      tagsDisplay.setVisibility(View.GONE);
      chipGroup.setVisibility(View.VISIBLE);
      chipGroup.removeAllViews();
      for (TagData tagData : orderByName.sortedCopy(selectedTags)) {
        if (tagData == null) {
          continue;
        }
        Chip chip = chipProvider.newClosableChip(tagData);
        chipProvider.apply(chip, tagData);
        chip.setOnClickListener(view -> onRowClick());
        chip.setOnCloseIconClickListener(
            view -> {
              selectedTags.remove(tagData);
              refreshDisplayView();
            });
        chipGroup.addView(chip);
      }
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == REQUEST_TAG_PICKER_ACTIVITY) {
      if (resultCode == RESULT_OK && data != null) {
        selectedTags = data.getParcelableArrayListExtra(TagPickerActivity.EXTRA_SELECTED);
        refreshDisplayView();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected void inject(FragmentComponent component) {
    component.inject(this);
  }

  @Override
  public boolean requiresId() {
    return true;
  }
}
