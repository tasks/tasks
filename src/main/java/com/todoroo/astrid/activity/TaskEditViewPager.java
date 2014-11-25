/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import org.tasks.R;

import java.util.ArrayList;

public class TaskEditViewPager extends PagerAdapter {

    private final String[] titles;
    public TaskEditFragment parent;

    public static final int TAB_SHOW_ACTIVITY = 1;

    public TaskEditViewPager(Context context, int tabStyleMask) {
        ArrayList<String> titleList = new ArrayList<>();
        if ((tabStyleMask & TAB_SHOW_ACTIVITY) > 0) {
            titleList.add(context.getString(R.string.TEA_tab_activity));
        }

        titles = titleList.toArray(new String[titleList.size()]);
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public Object instantiateItem(View pager, int position) {
        View pageView = parent.getPageView();

        ((ViewPager) pager).addView(pageView, 0);
        return pageView;
    }

    @Override
    public void destroyItem(View pager, int position, Object view) {
        ((ViewPager) pager).removeView((View) view);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

}
