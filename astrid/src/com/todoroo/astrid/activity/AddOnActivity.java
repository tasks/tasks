package com.todoroo.astrid.activity;


import android.app.TabActivity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.TabHost;

import com.timsu.astrid.R;

public class AddOnActivity extends TabActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up tab host
        Resources r = getResources();
        TabHost tabHost = getTabHost();
        tabHost.setPadding(0, 4, 0, 0);
        LayoutInflater.from(this).inflate(R.layout.addon_activity,
                tabHost.getTabContentView(), true);
        tabHost.addTab(tabHost.newTabSpec(r.getString(R.string.AOA_tab_installed)).
                setIndicator(r.getString(R.string.AOA_tab_installed),
                        r.getDrawable(R.drawable.tab_addons)).setContent(
                                R.id.installed_tab));
        tabHost.addTab(tabHost.newTabSpec(r.getString(R.string.AOA_tab_available)).
                setIndicator(r.getString(R.string.AOA_tab_available),
                        r.getDrawable(R.drawable.tab_add)).setContent(
                                R.id.availble_tab));

        setTitle(R.string.AOA_title);
    }

}
