package com.todoroo.astrid.activity;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.viewpagerindicator.TitleProvider;

public class TaskEditViewPager extends PagerAdapter implements TitleProvider {

    private static String[] titles = new String[] { "Activity", "More" };
    private final Context context;
    public TaskEditActivity parent;

    public TaskEditViewPager(Context context) {
        this.context = context;
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
    public void finishUpdate(View view) {
        //System.err.println("UpdateView");
    }


    @Override
    public Parcelable saveState() {
        return null;
    }

}