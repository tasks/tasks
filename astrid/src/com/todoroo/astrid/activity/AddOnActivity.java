package com.todoroo.astrid.activity;


import java.util.ArrayList;

import android.app.TabActivity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.adapter.AddOnAdapter;
import com.todoroo.astrid.model.AddOn;
import com.todoroo.astrid.service.AddOnService;
import com.todoroo.astrid.service.AstridDependencyInjector;

public class AddOnActivity extends TabActivity {

    /** boolean: whether to start on available page */
    public static final String TOKEN_START_WITH_AVAILABLE = "av"; //$NON-NLS-1$

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
                                R.id.installed));
        tabHost.addTab(tabHost.newTabSpec(r.getString(R.string.AOA_tab_available)).
                setIndicator(r.getString(R.string.AOA_tab_available),
                        r.getDrawable(R.drawable.tab_add)).setContent(
                                R.id.available));

        setTitle(R.string.AOA_title);

        populate();
    }

    private void populate() {
        AddOn[] list = addOnService.getAddOns();
        if(list == null)
            return;

        ArrayList<AddOn> installed = new ArrayList<AddOn>();
        ArrayList<AddOn> available = new ArrayList<AddOn>();

        for(AddOn addOn : list) {
            if(addOnService.isInstalled(addOn))
                installed.add(addOn);
            else
                available.add(addOn);
        }
        if(installed.size() == 0 || getIntent().getBooleanExtra(TOKEN_START_WITH_AVAILABLE, false))
            getTabHost().setCurrentTab(1);

        TextView noAddons = new TextView(this);
        noAddons.setText(R.string.TEA_no_addons);
        noAddons.setTextAppearance(this, R.style.TextAppearance_TLA_NoItems);

        ListView installedList = (ListView) findViewById(R.id.installed);
        installedList.setEmptyView(noAddons);
        installedList.setAdapter(new AddOnAdapter(this, true, installed));

        ListView availableList = (ListView) findViewById(R.id.available);
        availableList.setEmptyView(noAddons);
        availableList.setAdapter(new AddOnAdapter(this, false, available));
    }



}
