/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;


import java.util.ArrayList;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.adapter.AddOnAdapter;
import com.todoroo.astrid.data.AddOn;
import com.todoroo.astrid.service.AddOnService;
import com.todoroo.astrid.service.AstridDependencyInjector;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.utility.Constants;

/**
 * TODO: fix deprecation or get rid of me
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AddOnActivity extends SherlockFragmentActivity {

    /** boolean: whether to start on available page */
    public static final String TOKEN_START_WITH_AVAILABLE = "av"; //$NON-NLS-1$

    private View installedView;
    private View availableView;

    @Autowired
    AddOnService addOnService;

    static {
        AstridDependencyInjector.initialize();
    }

    public AddOnActivity() {
        DependencyInjectionService.getInstance().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeService.applyTheme(this);
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = LayoutInflater.from(this);
        installedView = inflater.inflate(R.layout.addon_list_container, null);
        availableView = inflater.inflate(R.layout.addon_list_container, null);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.Tab installedTab = ab.newTab().setText("  " + getString(R.string.AOA_tab_installed)) //$NON-NLS-1$
                                      .setIcon(R.drawable.gl_pencil)
                                      .setTabListener(new AddOnTabListener(installedView));

        ActionBar.Tab availableTab = ab.newTab().setText("  " + getString(R.string.AOA_tab_available)) //$NON-NLS-1$
                                                .setIcon(R.drawable.gl_more)
                                                .setTabListener(new AddOnTabListener(availableView));

        ab.addTab(availableTab);
        ab.addTab(installedTab);

        setTitle(R.string.AOA_title);

        populate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class AddOnTabListener implements ActionBar.TabListener {

        private final View mView;

        public AddOnTabListener(View v) {
            this.mView = v;
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            //
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            setContentView(mView);
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            //
        }

    }

    private void populate() {
        AddOn[] list = addOnService.getAddOns();
        if(list == null)
            return;

        ArrayList<AddOn> installed = new ArrayList<AddOn>();
        ArrayList<AddOn> available = new ArrayList<AddOn>();

        for(AddOn addOn : list) {
            if (AddOnService.POWER_PACK_PACKAGE.equals(addOn.getPackageName())) {
                if (addOnService.hasPowerPack())
                    installed.add(addOn);
                else if (Constants.MARKET_STRATEGY.generateMarketLink(addOn.getPackageName()) != null)
                    available.add(addOn);
            } else {
                if(addOnService.isInstalled(addOn))
                    installed.add(addOn);
                else if (Constants.MARKET_STRATEGY.generateMarketLink(addOn.getPackageName()) != null)
                    available.add(addOn);

            }
        }

        ListView installedList = (ListView) installedView.findViewById(R.id.list);
        installedList.setAdapter(new AddOnAdapter(this, true, installed));
        if(installed.size() > 0)
            installedView.findViewById(R.id.empty).setVisibility(View.GONE);

        ListView availableList = (ListView) availableView.findViewById(R.id.list);
        availableList.setAdapter(new AddOnAdapter(this, false, available));
        if(available.size() > 0)
            availableView.findViewById(R.id.empty).setVisibility(View.GONE);
    }

    /**
     * Creates an on click listener
     * @param activity
     * @param finish whether to finish activity
     * @return
     */
    public static DialogInterface.OnClickListener createAddOnClicker(final Activity activity,
            final boolean finish) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(activity,
                        AddOnActivity.class);
                intent.putExtra(AddOnActivity.TOKEN_START_WITH_AVAILABLE, true);
                activity.startActivity(intent);
                if(finish)
                    activity.finish();
            }
        };
    }

}
