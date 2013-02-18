/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.commonsware.cwac.tlv.TouchListView;
import com.commonsware.cwac.tlv.TouchListView.DropListener;
import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

public class BeastModePreferences extends ListActivity {

    private TouchListView touchList;
    private ArrayAdapter<String> adapter;

    private ArrayList<String> items;

    public static final String BEAST_MODE_ORDER_PREF = "beast_mode_order"; //$NON-NLS-1$

    public static final String BEAST_MODE_PREF_ITEM_SEPARATOR = ";"; //$NON-NLS-1$

    private static final String BEAST_MODE_ASSERTED_HIDE_ALWAYS = "asserted_hide_always"; //$NON-NLS-1$

    private HashMap<String, String> prefsToDescriptions;

    /**
     * Migration for existing users to assert that the "hide always" section divider exists in the preferences.
     * Knowing that this section will always be in the constructed list of controls simplifies the logic a bit.
     * @param c
     */
    public static void assertHideUntilSectionExists(Context c, long latestSetVersion) {
        if (latestSetVersion == 0)
            Preferences.setBoolean(BEAST_MODE_ASSERTED_HIDE_ALWAYS, true);

        if (Preferences.getBoolean(BEAST_MODE_ASSERTED_HIDE_ALWAYS, false))
            return;

        String order = Preferences.getStringValue(BEAST_MODE_ORDER_PREF);
        String hideSectionPref = c.getString(R.string.TEA_ctrl_hide_section_pref);
        if (TextUtils.isEmpty(order)) {
            // create preference and stick hide always at the end of it
            String[] items = c.getResources().getStringArray(R.array.TEA_control_sets_prefs);
            StringBuilder builder = new StringBuilder();
            for (String item : items) {
                if (item.equals(hideSectionPref))
                    continue;
                builder.append(item);
                builder.append(BEAST_MODE_PREF_ITEM_SEPARATOR);
            }

            builder.append(hideSectionPref);
            builder.append(BEAST_MODE_PREF_ITEM_SEPARATOR);
            order = builder.toString();
        } else if (!order.contains(hideSectionPref)) {
            order += (hideSectionPref + BEAST_MODE_PREF_ITEM_SEPARATOR);
        }
        Preferences.setString(BEAST_MODE_ORDER_PREF, order);

        Preferences.setBoolean(BEAST_MODE_ASSERTED_HIDE_ALWAYS, true);
    }

    public static void setDefaultLiteModeOrder(Context context, boolean force) {
        if (Preferences.getStringValue(BEAST_MODE_ORDER_PREF) != null && !force)
            return;

        if (force)
            Preferences.clear(BEAST_MODE_ORDER_PREF);
        ArrayList<String> list = constructOrderedControlList(context);
        String moreSeparator = context.getResources().getString(R.string.TEA_ctrl_more_pref);
        String hideSeparator = context.getResources().getString(R.string.TEA_ctrl_hide_section_pref);
        String importancePref = context.getResources().getString(R.string.TEA_ctrl_importance_pref);
        String listsPref = context.getResources().getString(R.string.TEA_ctrl_lists_pref);

        list.remove(importancePref);
        list.remove(listsPref);
        int moreIndex = list.indexOf(moreSeparator);
        if (moreIndex >= 0) {
            list.add(moreIndex + 1, listsPref);
            list.add(moreIndex + 1, importancePref);
        }

        list.remove(hideSeparator);
        moreIndex = list.indexOf(moreSeparator);
        if (moreIndex >= 0)
            list.add(moreIndex, hideSeparator);
        else
            list.add(hideSeparator);

        StringBuilder newSetting = new StringBuilder(30);
        for (String item : list) {
            newSetting.append(item);
            newSetting.append(BEAST_MODE_PREF_ITEM_SEPARATOR);
        }
        Preferences.setString(BEAST_MODE_ORDER_PREF, newSetting.toString());
    }

    public static void setDefaultOrder(Context context, boolean force) {
        if (Preferences.getStringValue(BEAST_MODE_ORDER_PREF) != null && !force)
            return;

        if (force)
            Preferences.clear(BEAST_MODE_ORDER_PREF);
        ArrayList<String> list = constructOrderedControlList(context);
        StringBuilder newSetting = new StringBuilder(30);
        for (String item : list) {
            newSetting.append(item);
            newSetting.append(BEAST_MODE_PREF_ITEM_SEPARATOR);
        }
        Preferences.setString(BEAST_MODE_ORDER_PREF, newSetting.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.beast_mode_pref_activity);
        setTitle(R.string.EPr_beastMode_desc);

        prefsToDescriptions = new HashMap<String, String>();
        buildDescriptionMap(getResources());

        touchList = (TouchListView) getListView();
        items = constructOrderedControlList(this);

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
        while (items.size() > 0)
            items.remove(0);
        for (String s : prefsArray)
            items.add(s);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void finish() {
        StringBuilder newSetting = new StringBuilder(30);
        for (int i = 0; i < adapter.getCount(); i++) {
            newSetting.append(adapter.getItem(i));
            newSetting.append(BEAST_MODE_PREF_ITEM_SEPARATOR);
        }
        Preferences.setString(BEAST_MODE_ORDER_PREF, newSetting.toString());
        super.finish();
    }

    public static ArrayList<String> constructOrderedControlList(Context context) {
        String order = Preferences.getStringValue(BEAST_MODE_ORDER_PREF);
        ArrayList<String> list = new ArrayList<String>();
        String[] itemsArray;
        if (order == null) {
            itemsArray = context.getResources().getStringArray(R.array.TEA_control_sets_prefs);
        } else {
            itemsArray = order.split(BEAST_MODE_PREF_ITEM_SEPARATOR);
        }

        for (String s : itemsArray) {
            if (!s.equals(context.getString(R.string.TEA_ctrl_title_pref)) && !s.equals(context.getString(R.string.TEA_ctrl_share_pref))) {
                list.add(s);
            }
        }

        if (order == null)
            return list;

        itemsArray = context.getResources().getStringArray(R.array.TEA_control_sets_prefs);
        for (int i = 0; i < itemsArray.length; i++) {
            if (!list.contains(itemsArray[i]))
                list.add(i, itemsArray[i]);
        }
        return list;
    }

}
