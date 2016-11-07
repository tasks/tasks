package org.tasks.tasklist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.TagFilter;
import com.todoroo.astrid.data.TagData;

import org.tasks.R;
import org.tasks.activities.TagSettingsActivity;
import org.tasks.injection.FragmentComponent;

public class TagListFragment extends TaskListFragment {

    private static final int REQUEST_EDIT_TAG = 11543;

    public static TaskListFragment newTagViewFragment(TagFilter filter, TagData tagData) {
        TagListFragment fragment = new TagListFragment();
        fragment.filter = filter;
        fragment.tagData = tagData;
        return fragment;
    }

    private static final String EXTRA_TAG_DATA = "extra_tag_data";

    protected TagData tagData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            tagData = savedInstanceState.getParcelable(EXTRA_TAG_DATA);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void inflateMenu(Toolbar toolbar) {
        super.inflateMenu(toolbar);
        toolbar.inflateMenu(R.menu.menu_tag_view_fragment);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tag_settings:
                Intent intent = new Intent(getActivity(), TagSettingsActivity.class);
                intent.putExtra(TagSettingsActivity.EXTRA_TAG_DATA, tagData);
                startActivityForResult(intent, REQUEST_EDIT_TAG);
                return true;
            default:
                return super.onMenuItemClick(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT_TAG) {
            if (resultCode == Activity.RESULT_OK) {
                String action = data.getAction();
                TaskListActivity activity = (TaskListActivity) getActivity();
                if (TagSettingsActivity.ACTION_DELETED.equals(action)) {
                    activity.onFilterItemClicked(null);
                } else if (TagSettingsActivity.ACTION_RELOAD.equals(action)) {
                    activity.getIntent().putExtra(TaskListActivity.OPEN_FILTER,
                            (Filter) data.getParcelableExtra(TaskListActivity.OPEN_FILTER));
                    activity.recreate();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(EXTRA_TAG_DATA, tagData);
    }

    @Override
    protected boolean hasDraggableOption() {
        return tagData != null;
    }

    @Override
    public void inject(FragmentComponent component) {
        component.inject(this);
    }
}
