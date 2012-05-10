package com.todoroo.astrid.people;

import android.content.Intent;
import android.os.Bundle;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import com.todoroo.astrid.api.FilterWithUpdate;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.ui.MainMenuPopover;

public class PeopleViewActivity extends TaskListActivity {

    private AsyncImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imageView = (AsyncImageView) findViewById(R.id.person_image);
        imageView.setDefaultImageResource(R.drawable.icn_default_person_image);
    }

    @Override
    protected int getContentView() {
        return R.layout.people_view_wrapper_activity;
    }

    @Override
    protected Filter getDefaultFilter() {
        return PeopleFilterExposer.mySharedTasks(this);
    }

    @Override
    protected Class<? extends FilterListFragment> getFilterListClass() {
        return PeopleListFragment.class;
    }

    @Override
    protected int getHeaderView() {
        return R.layout.header_people_view;
    }

    @Override
    public boolean onFilterItemClicked(FilterListItem item) {
        boolean result = super.onFilterItemClicked(item);
        if (result && item instanceof FilterWithUpdate)
            imageView.setUrl(((FilterWithUpdate) item).imageUrl);
        else
            imageView.setUrl(null);
        return result;
    }

    private static final int[] FORBIDDEN_MENU_ITEMS = {
        TaskListFragment.MENU_NEW_FILTER_ID,
        TaskListFragment.MENU_ADDONS_ID,
        MainMenuPopover.MAIN_MENU_ITEM_FRIENDS
    };

    @Override
    public boolean shouldAddMenuItem(int itemId) {
        return AndroidUtilities.indexOf(FORBIDDEN_MENU_ITEMS, itemId) < 0;
    }

    @Override
    public void mainMenuItemSelected(int item, Intent customIntent) {
        if (item == MainMenuPopover.MAIN_MENU_ITEM_LISTS) {
            finish();
            return;
        }

        super.mainMenuItemSelected(item, customIntent);
    }
}
