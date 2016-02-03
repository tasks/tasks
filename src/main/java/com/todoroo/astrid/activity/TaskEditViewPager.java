/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

public class TaskEditViewPager extends PagerAdapter {

    public TaskEditFragment parent;

    @Override
    public int getCount() {
        return 1;
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
