package com.todoroo.astrid.activity;

import android.content.Context;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;


public interface FilterModeSpec {

    public int[] getForbiddenMenuItems();
    public Class<? extends FilterListFragment> getFilterListClass();
    public Filter getDefaultFilter(Context context);
    public int getMainMenuIconAttr();
    public void onFilterItemClickedCallback(FilterListItem item);
    public boolean showComments();

}
