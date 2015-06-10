/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.commonsware.cwac.tlv.TouchListView;
import com.commonsware.cwac.tlv.TouchListView.DropListener;

import org.tasks.R;
import org.tasks.injection.InjectingListActivity;
import org.tasks.preferences.ActivityPreferences;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.inject.Inject;

public class BeastModePreferences extends InjectingListActivity {

    private ArrayAdapter<String> adapter;

    private ArrayList<String> items;

    public static final String BEAST_MODE_ORDER_PREF = "beast_mode_order_v2"; //$NON-NLS-1$

    public static final String BEAST_MODE_PREF_ITEM_SEPARATOR = ";"; //$NON-NLS-1$

    private HashMap<String, String> prefsToDescriptions;

    @Inject ActivityPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences.applyLightStatusBarColor();
        setContentView(R.layout.beast_mode_pref_activity);
        setTitle(R.string.EPr_beastMode_desc);

        prefsToDescriptions = new HashMap<>();
        buildDescriptionMap(getResources());

        TouchListView touchList = (TouchListView) getListView();
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
        touchList.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        touchList.setDropListener(new DropListener() {
            @Override
            public void drop(int from, int to) {
                String s = items.remove(from);
                items.add(to, s);
                adapter.notifyDataSetChanged();
            }
        });

        Button resetButton = (Button) findViewById(R.id.reset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetToDefault();
            }
        });
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
        StringBuilder newSetting = new StringBuilder(30);
        for (int i = 0; i < adapter.getCount(); i++) {
            newSetting.append(adapter.getItem(i));
            newSetting.append(BEAST_MODE_PREF_ITEM_SEPARATOR);
        }
        preferences.setString(BEAST_MODE_ORDER_PREF, newSetting.toString());
        super.finish();
    }

    public static void setDefaultOrder(Preferences preferences, Context context) {
        if (preferences.getStringValue(BEAST_MODE_ORDER_PREF) != null) {
            return;
        }

        ArrayList<String> list = constructOrderedControlList(preferences, context);
        StringBuilder newSetting = new StringBuilder(30);
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

        for (String s : itemsArray) {
            if (!s.equals(context.getString(R.string.TEA_ctrl_title_pref)) &&
                    !s.equals(context.getString(R.string.TEA_ctrl_share_pref)) &&
                    !s.equals(context.getString(R.string.TEA_ctrl_more_pref))) {
                list.add(s);
            }
        }

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
