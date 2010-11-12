package com.todoroo.astrid.activity;


import java.util.ArrayList;

import android.app.Activity;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;

import com.timsu.astrid.R;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.adapter.AddOnAdapter;
import com.todoroo.astrid.data.AddOn;
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
                                R.id.tab_installed));
        tabHost.addTab(tabHost.newTabSpec(r.getString(R.string.AOA_tab_available)).
                setIndicator(r.getString(R.string.AOA_tab_available),
                        r.getDrawable(R.drawable.tab_add)).setContent(
                                R.id.tab_available));
        getTabWidget().setBackgroundColor(Color.BLACK);

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

        ListView installedList = (ListView) findViewById(R.id.installed);
        installedList.setAdapter(new AddOnAdapter(this, true, installed));
        if(installed.size() > 0)
            findViewById(R.id.empty_installed).setVisibility(View.GONE);

        ListView availableList = (ListView) findViewById(R.id.available);
        availableList.setAdapter(new AddOnAdapter(this, false, available));
        if(available.size() > 0)
            findViewById(R.id.empty_available).setVisibility(View.GONE);
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
