package org.tasks.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import com.todoroo.astrid.core.SortHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.InjectingFragmentActivity;
import org.tasks.preferences.Preferences;

import javax.inject.Inject;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class SortActivity extends InjectingFragmentActivity {

    private static final Logger log = LoggerFactory.getLogger(SortActivity.class);

    public static final String EXTRA_MANUAL_ENABLED = "extra_manual_enabled";
    public static final String EXTRA_TOGGLE_MANUAL = "extra_toggle_manual";

    @Inject Preferences preferences;
    @Inject DialogBuilder dialogBuilder;

    private boolean manualEnabled;
    private AlertDialog alertDialog;
    private int selectedIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manualEnabled = getIntent().getBooleanExtra(EXTRA_MANUAL_ENABLED, false);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice);

        if (manualEnabled) {
            adapter.add(getString(R.string.SSD_sort_drag));
        }

        adapter.add(getString(R.string.SSD_sort_auto));
        adapter.add(getString(R.string.SSD_sort_due));
        adapter.add(getString(R.string.SSD_sort_importance));
        adapter.add(getString(R.string.SSD_sort_alpha));
        adapter.add(getString(R.string.SSD_sort_modified));

        selectedIndex = getIndex(preferences.getSortMode());
        if (manualEnabled) {
            if (preferences.getBoolean(R.string.p_manual_sort, false)) {
                selectedIndex = 0;
            }
        } else {
            selectedIndex -= 1;
        }

        alertDialog = dialogBuilder.newDialog()
                .setSingleChoiceItems(adapter, selectedIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedIndex = which;
                        enableReverse();
                    }
                })
                .setPositiveButton(R.string.TLA_menu_sort, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setSelection(false);
                    }
                })
                .setNeutralButton(R.string.reverse, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setSelection(true);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .show();

        enableReverse();
    }

    private void enableReverse() {
        if (manualEnabled) {
            Button reverse = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            if (atLeastLollipop()) {
                reverse.setEnabled(selectedIndex != 0);
            } else {
                reverse.setVisibility(selectedIndex == 0 ? View.GONE : View.VISIBLE);
            }
        }
    }

    private void setSelection(boolean reverse) {
        preferences.setBoolean(R.string.p_reverse_sort, reverse);

        final boolean wasManual = preferences.getBoolean(R.string.p_manual_sort, false);
        final boolean isManual = manualEnabled && selectedIndex == 0;

        preferences.setBoolean(R.string.p_manual_sort, isManual);

        if (!isManual) {
            preferences.setSortMode(getSortMode(manualEnabled ? selectedIndex : selectedIndex + 1));
        }

        setResult(RESULT_OK, new Intent() {{
            if (wasManual != isManual) {
                putExtra(EXTRA_TOGGLE_MANUAL, isManual);
            }
        }});

        finish();
    }

    private int getIndex(int sortMode) {
        switch (sortMode) {
            case SortHelper.SORT_AUTO:
                return 1;
            case SortHelper.SORT_DUE:
                return 2;
            case SortHelper.SORT_IMPORTANCE:
                return 3;
            case SortHelper.SORT_ALPHA:
                return 4;
            case SortHelper.SORT_MODIFIED:
                return 5;
        }

        log.error("Invalid sort mode: {}", sortMode);
        return 1;
    }

    private int getSortMode(int index) {
        switch (index) {
            case 1:
                return SortHelper.SORT_AUTO;
            case 2:
                return SortHelper.SORT_DUE;
            case 3:
                return SortHelper.SORT_IMPORTANCE;
            case 4:
                return SortHelper.SORT_ALPHA;
            case 5:
                return SortHelper.SORT_MODIFIED;
        }

        log.error("Invalid sort mode: {}", index);
        return SortHelper.SORT_ALPHA;
    }
}
