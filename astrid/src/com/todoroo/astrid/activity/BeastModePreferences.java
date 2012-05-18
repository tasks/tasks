package com.todoroo.astrid.activity;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
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
import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

public class BeastModePreferences extends ListActivity {

    private TouchListView touchList;
    private ArrayAdapter<String> adapter;

    private ArrayList<String> items;

    public static final String BEAST_MODE_ORDER_PREF = "beast_mode_order"; //$NON-NLS-1$

    public static final String BEAST_MODE_PREF_ITEM_SEPARATOR = ";"; //$NON-NLS-1$

    private HashMap<String, String> prefsToDescriptions;

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
            if (!s.equals(context.getResources().getString(R.string.TEA_ctrl_title_pref))){
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
