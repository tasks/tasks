/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import java.util.ArrayList;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.timsu.astrid.R;
import com.viewpagerindicator.TitleProvider;

public class TaskEditViewPager extends PagerAdapter implements TitleProvider {

    private final String[] titles;
    public TaskEditFragment parent;

    public static final int TAB_SHOW_ACTIVITY = 1 << 0;
    public static final int TAB_SHOW_MORE = 1 << 1;

    public TaskEditViewPager(Context context, int tabStyleMask) {
        ArrayList<String> titleList = new ArrayList<String>();
        if ((tabStyleMask & TAB_SHOW_ACTIVITY) > 0)
            titleList.add(context.getString(R.string.TEA_tab_activity));
        if ((tabStyleMask & TAB_SHOW_MORE) > 0)
            titleList.add(context.getString(R.string.TEA_tab_more));

        titles = titleList.toArray(new String[titleList.size()]);
    }

    public static int getPageForPosition(int position, int tabStyle) {
        int numOnesEncountered = 0;
        for (int i = 0; i <= 2; i++) {
            if ((tabStyle & (1 << i)) > 0)
                numOnesEncountered++;
            if (numOnesEncountered == position + 1)
                return 1 << i;
        }
        return -1;
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
