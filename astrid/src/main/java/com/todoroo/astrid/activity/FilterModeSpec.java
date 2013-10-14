package com.todoroo.astrid.activity;

import android.content.Context;

import com.todoroo.astrid.api.Filter;


public interface FilterModeSpec {

    public Class<? extends FilterListFragment> getFilterListClass();
    public Filter getDefaultFilter(Context context);

}
