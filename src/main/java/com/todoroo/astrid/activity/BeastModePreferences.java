/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.commonsware.cwac.tlv.TouchListView;
import com.google.common.base.Strings;

import org.tasks.R;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.ui.MenuColorizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BeastModePreferences extends ThemedInjectingAppCompatActivity implements Toolbar.OnMenuItemClickListener {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(android.R.id.list) TouchListView touchList;

    private ArrayAdapter<String> adapter;

    private ArrayList<String> items;

    public static final String BEAST_MODE_ORDER_PREF = "beast_mode_order_v3"; //$NON-NLS-1$

    public static final String BEAST_MODE_PREF_ITEM_SEPARATOR = ";"; //$NON-NLS-1$

    private HashMap<String, String> prefsToDescriptions;

    @Inject Preferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.beast_mode_pref_activity);
        ButterKnife.bind(this);

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_24dp));
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.beast_mode);
        toolbar.setOnMenuItemClickListener(this);
        MenuColorizer.colorToolbar(this, toolbar);

        prefsToDescriptions = new HashMap<>();
        buildDescriptionMap(getResources());

        items = constructOrderedControlList(preferences, this);

        adapter = new ArrayAdapter<String>(this, R.layout.preference_draggable_row, R.id.text, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView display = (TextView) v.findViewById(R.id.text);
                display.setText(prefsToDescriptions.get(getItem(position)));
                return v;
            }
        };
        touchList.setAdapter(adapter);
        touchList.setOnTouchListener((v, event) -> false);
        touchList.setDropListener((from, to) -> {
            String s = items.remove(from);
            items.add(to, s);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_reset_to_defaults:
                resetToDefault();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void buildDescriptionMap(Resources r) {
        String[] keys = r.getStringArray(R.array.TEA_control_sets_prefs);
        String[] descriptions = r.getStringArray(R.array.TEA_control_sets_beast);
        for (int i = 0; i < keys.length && i < descriptions.length; i++) {
            prefsToDescriptions.put(keys[i], descriptions[i]);
        }
    }

    private void resetToDefault() {
        String[] prefsArray = getResources().getStringArray(R.array.TEA_control_sets_prefs);
        while (items.size() > 0) {
            items.remove(0);
        }
        Collections.addAll(items, prefsArray);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void finish() {
        StringBuilder newSetting = new StringBuilder();
        for (int i = 0; i < adapter.getCount(); i++) {
            newSetting.append(adapter.getItem(i));
            newSetting.append(BEAST_MODE_PREF_ITEM_SEPARATOR);
        }
        String oldValue = preferences.getStringValue(BEAST_MODE_ORDER_PREF);
        String newValue = newSetting.toString();
        if (Strings.isNullOrEmpty(oldValue) || !oldValue.equals(newValue)) {
            preferences.setString(BEAST_MODE_ORDER_PREF, newSetting.toString());
            setResult(RESULT_OK);
        }
        super.finish();
    }

    public static void setDefaultOrder(Preferences preferences, Context context) {
        if (preferences.getStringValue(BEAST_MODE_ORDER_PREF) != null) {
            return;
        }

        ArrayList<String> list = constructOrderedControlList(preferences, context);
        StringBuilder newSetting = new StringBuilder();
        for (String item : list) {
            newSetting.append(item);
            newSetting.append(BEAST_MODE_PREF_ITEM_SEPARATOR);
        }
        preferences.setString(BEAST_MODE_ORDER_PREF, newSetting.toString());
    }

    public static ArrayList<String> constructOrderedControlList(Preferences preferences, Context context) {
        String order = preferences.getStringValue(BEAST_MODE_ORDER_PREF);
        ArrayList<String> list = new ArrayList<>();
        String[] itemsArray;
        if (order == null) {
            itemsArray = context.getResources().getStringArray(R.array.TEA_control_sets_prefs);
        } else {
            itemsArray = order.split(BEAST_MODE_PREF_ITEM_SEPARATOR);
        }

        Collections.addAll(list, itemsArray);

        if (order == null) {
            return list;
        }

        itemsArray = context.getResources().getStringArray(R.array.TEA_control_sets_prefs);
        for (int i = 0; i < itemsArray.length; i++) {
            if (!list.contains(itemsArray[i])) {
                list.add(i, itemsArray[i]);
            }
        }
        return list;
    }
}
