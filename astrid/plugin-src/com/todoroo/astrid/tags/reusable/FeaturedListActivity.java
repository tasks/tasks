package com.todoroo.astrid.tags.reusable;

import android.content.Intent;

import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.activity.FilterListFragment;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.activity.TaskListFragment;
import com.todoroo.astrid.ui.MainMenuPopover;

public class FeaturedListActivity extends TaskListActivity {

    @Override
    protected Class<? extends FilterListFragment> getFilterListClass() {
        return FeaturedListFragment.class;
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
