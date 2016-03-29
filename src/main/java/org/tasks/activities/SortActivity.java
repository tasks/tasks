package org.tasks.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import com.todoroo.astrid.core.SortHelper;

import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class SortActivity extends InjectingAppCompatActivity {

    public static final String EXTRA_MANUAL_ENABLED = "extra_manual_enabled";

    @Inject Preferences preferences;
    @Inject DialogBuilder dialogBuilder;

    private boolean manualEnabled;
    private AlertDialog alertDialog;
    private int selectedIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        manualEnabled = getIntent().getBooleanExtra(EXTRA_MANUAL_ENABLED, false);

        List<String> items = new ArrayList<>();

        if (manualEnabled) {
            items.add(getString(R.string.SSD_sort_drag));
        }

        items.add(getString(R.string.SSD_sort_auto));
        items.add(getString(R.string.SSD_sort_due));
        items.add(getString(R.string.SSD_sort_importance));
        items.add(getString(R.string.SSD_sort_alpha));
        items.add(getString(R.string.SSD_sort_modified));

        selectedIndex = getIndex(preferences.getSortMode());
        if (manualEnabled) {
            if (preferences.getBoolean(R.string.p_manual_sort, false)) {
                selectedIndex = 0;
            }
        } else {
            selectedIndex -= 1;
        }

        alertDialog = dialogBuilder.newDialog()
                .setSingleChoiceItems(items.toArray(new String[items.size()]), selectedIndex, new DialogInterface.OnClickListener() {
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

    @Override
    public void inject(ActivityComponent component) {
        component.inject(this);
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

        final boolean isManual = manualEnabled && selectedIndex == 0;

        preferences.setBoolean(R.string.p_manual_sort, isManual);

        if (!isManual) {
            preferences.setSortMode(getSortMode(manualEnabled ? selectedIndex : selectedIndex + 1));
        }

        setResult(RESULT_OK);

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

        Timber.e("Invalid sort mode: %s", sortMode);
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

        Timber.e("Invalid sort mode: %s", index);
        return SortHelper.SORT_ALPHA;
    }
}
