package com.todoroo.astrid.activity;

import java.util.ArrayList;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.commonsware.cwac.tlv.TouchListView;
import com.commonsware.cwac.tlv.TouchListView.DropListener;
import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;

public class BeastModePreferenceActivity extends ListActivity {

    private TouchListView touchList;
    private ArrayAdapter<String> adapter;

    private ArrayList<String> items;

    public static final String BEAST_MODE_ORDER_PREF = "beast_mode_order"; //$NON-NLS-1$

    public static final String BEAST_MODE_PREF_ITEM_SEPARATOR = ";"; //$NON-NLS-1$

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.beast_mode_pref_activity);
        setTitle(R.string.EPr_beastMode_desc);

        touchList = (TouchListView) getListView();
        String order = Preferences.getStringValue(BEAST_MODE_ORDER_PREF);
        String[] itemsArray;
        if (order == null) {
            itemsArray = getResources().getStringArray(R.array.TEA_control_sets);
        } else {
            itemsArray = order.split(BEAST_MODE_PREF_ITEM_SEPARATOR);
        }

        items = new ArrayList<String>();
        for (String s : itemsArray) {
            items.add(s);
        }

        adapter = new ArrayAdapter<String>(this, R.layout.preference_draggable_row, R.id.text, items);
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

    private void resetToDefault() {
        String[] itemsArray = getResources().getStringArray(R.array.TEA_control_sets);
        while (items.size() > 0)
            items.remove(0);
        for (String s : itemsArray)
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

}
