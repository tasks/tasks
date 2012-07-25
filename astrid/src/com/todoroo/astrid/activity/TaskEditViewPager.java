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

import com.timsu.astrid.R;
import com.viewpagerindicator.TitleProvider;

public class TaskEditViewPager extends PagerAdapter implements TitleProvider {

    private static String[] titles;
    public TaskEditFragment parent;

    public TaskEditViewPager(Context context, int tabStyle) {
        switch(tabStyle) {
        case TaskEditFragment.TAB_STYLE_ACTIVITY_WEB:
            titles = new String[] {
                    context.getString(R.string.TEA_tab_activity),
                    context.getString(R.string.TEA_tab_more),
                    context.getString(R.string.TEA_tab_web),
            };
            break;
        case TaskEditFragment.TAB_STYLE_ACTIVITY:
            titles = new String[] {
                context.getString(R.string.TEA_tab_activity),
                context.getString(R.string.TEA_tab_more),
            };
            break;
        case TaskEditFragment.TAB_STYLE_WEB:
            titles = new String[] {
                    context.getString(R.string.TEA_tab_more),
                    context.getString(R.string.TEA_tab_web),
            };
            break;
        }
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public Object instantiateItem(View pager, int position) {
        View pageView = parent.getPageView(position);

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
    public String getTitle(int position) {
        return titles[position];
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

}
