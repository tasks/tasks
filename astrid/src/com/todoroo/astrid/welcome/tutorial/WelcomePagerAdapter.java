/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.welcome.tutorial;


import android.accounts.Account;
import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
import com.todoroo.astrid.utility.Constants;
import com.viewpagerindicator.TitleProvider;

public class WelcomePagerAdapter extends PagerAdapter implements TitleProvider
{
    private int[] images = new int[] {
        R.drawable.welcome_walkthrough_1,
        R.drawable.welcome_walkthrough_2,
        R.drawable.welcome_walkthrough_3,
        R.drawable.welcome_walkthrough_4,
        R.drawable.welcome_walkthrough_5,
        R.drawable.welcome_walkthrough_6,
        0
    };
    private int[] title = new int[] {
        R.string.welcome_title_1,
        R.string.welcome_title_2,
        R.string.welcome_title_3,
        R.string.welcome_title_4,
        R.string.welcome_title_5,
        R.string.welcome_title_6,
        R.string.welcome_title_7,
    };
    private int[] body = new int[] {
        R.string.welcome_body_1,
        R.string.welcome_body_2,
        R.string.welcome_body_3,
        R.string.welcome_body_4,
        R.string.welcome_body_5,
        R.string.welcome_body_6,
        R.string.welcome_body_7,
    };
    public int[] layouts = new int[] {
        R.layout.welcome_walkthrough_page,
        R.layout.welcome_walkthrough_page,
        R.layout.welcome_walkthrough_page,
        R.layout.welcome_walkthrough_page,
        R.layout.welcome_walkthrough_page,
        R.layout.welcome_walkthrough_page,
        R.layout.actfm_login_activity,
    };

    public int fallbackLoginPage;

    private final Context context;
    public Account[] accounts;
    public WelcomeWalkthrough parent;
    @Autowired ActFmPreferenceService actFmPreferenceService;

    public WelcomePagerAdapter(Context context, boolean manual) {
        this.context = context;
        DependencyInjectionService.getInstance().inject(this);

        accounts = null;
        try {
            GoogleAccountManager am = new GoogleAccountManager(context);
            accounts = am.getAccounts();
        } catch (Exception e) {
            //
        }

        if (manual) {
            layouts[layouts.length - 1] = R.layout.welcome_walkthrough_page;
            title[title.length - 1] = R.string.welcome_title_7_return;
            images[images.length - 1] = R.drawable.welcome_walkthrough_1;
            body[body.length - 1] = R.string.welcome_body_7_return;
            fallbackLoginPage = R.layout.actfm_login_activity;
        } else {
            if (Constants.ASTRID_LITE) {
                adjustResourcesForLite();
            }

            // Setup login page from AB tests
            fallbackLoginPage = layouts[layouts.length - 1];
            if (accounts != null && accounts.length > 0) { // If has google account
                layouts[layouts.length - 1] = R.layout.welcome_walkthrough_simple_login;
                fallbackLoginPage = R.layout.actfm_login_activity;
            } else { // If no google account
                fallbackLoginPage = layouts[layouts.length - 1] = R.layout.actfm_login_activity;
            }
        }
    }

    private void adjustResourcesForLite() {
        images = new int[] { 0 };
        title = new int[] { R.string.welcome_title_7 };
        body = new int[] { R.string.welcome_body_7 };
        layouts = new int[] { R.layout.actfm_login_activity };
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);

        parent.onPageChanged(container, position);
    }


    @Override
    public int getCount() {
        return layouts.length;
    }

    public void changeLoginPage(int newLayout) {
        layouts[layouts.length - 1] = newLayout;
        notifyDataSetChanged();
    }

    @Override
    public Object instantiateItem(final View pager, final int position) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View pageView = inflater.inflate(layouts[position], null, true);
        pageView.setLayoutParams(new ViewGroup.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT));

        if (pageView.findViewById(R.id.welcome_walkthrough_image) != null) {
            ImageView imageView = (ImageView) pageView.findViewById(R.id.welcome_walkthrough_image);
            imageView.setImageResource(images[position]);

            TextView titleView = (TextView) pageView.findViewById(R.id.welcome_walkthrough_title);
            titleView.setText(title[position]);

            TextView bodyView = (TextView) pageView.findViewById(R.id.welcome_walkthrough_body);
            bodyView.setText(body[position]);
        }

        ((ViewPager) pager).addView(pageView, 0);
        parent.instantiatePage(position);
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
    public void finishUpdate(View view) {
        //
    }

    @Override
    public void restoreState(Parcelable p, ClassLoader c) {
        //
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void startUpdate(View view) {
        //
    }

    @Override
    public String getTitle(int position) {
        return context.getString(title[position]);
    }

}
