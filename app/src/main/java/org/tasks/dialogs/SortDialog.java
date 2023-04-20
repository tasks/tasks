package org.tasks.dialogs;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.SortHelper;

import org.tasks.R;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.QueryPreferences;
import org.tasks.widget.WidgetPreferences;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class SortDialog extends DialogFragment {

  private static final String EXTRA_MANUAL_ENABLED = "extra_manual_enabled";
  private static final String EXTRA_ASTRID_ENABLED = "extra_astrid_enabled";
  private static final String EXTRA_SELECTED_INDEX = "extra_selected_index";
  private static final String EXTRA_WIDGET_ID = "extra_widget_id";

  @Inject Preferences appPreferences;
  @Inject DialogBuilder dialogBuilder;

  private QueryPreferences preferences;
  private boolean manualEnabled;
  private boolean astridEnabled;
  private int selectedIndex;
  private AlertDialog alertDialog;
  private SortDialogCallback callback;

  public static SortDialog newSortDialog(Filter filter) {
    SortDialog sortDialog = new SortDialog();
    Bundle args = new Bundle();
    args.putBoolean(EXTRA_MANUAL_ENABLED, filter.supportsManualSort());
    args.putBoolean(EXTRA_ASTRID_ENABLED, filter.supportsAstridSorting());
    sortDialog.setArguments(args);
    return sortDialog;
  }

  public static SortDialog newSortDialog(Fragment target, int rc, Filter filter, int widgetId) {
    SortDialog dialog = newSortDialog(filter);
    dialog.setTargetFragment(target, rc);
    dialog.getArguments().putInt(EXTRA_WIDGET_ID, widgetId);
    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    onCreate(savedInstanceState);

    Bundle arguments = getArguments();
    int widgetId = arguments.getInt(EXTRA_WIDGET_ID, -1);
    preferences = widgetId < 0
        ? appPreferences
        : new WidgetPreferences(getContext(), appPreferences, widgetId);
    manualEnabled = arguments.getBoolean(EXTRA_MANUAL_ENABLED);
    astridEnabled = arguments.getBoolean(EXTRA_ASTRID_ENABLED)
        && appPreferences.getBoolean(R.string.p_astrid_sort_enabled, false);

    if (savedInstanceState != null) {
      selectedIndex = savedInstanceState.getInt(EXTRA_SELECTED_INDEX);
    } else {
      selectedIndex = getIndex(preferences.getSortMode());
    }

    List<String> items = new ArrayList<>();

    if (manualEnabled) {
      items.add(getString(R.string.SSD_sort_my_order));
    } else if (astridEnabled) {
      items.add(getString(R.string.astrid_sort_order));
    }

    items.add(getString(R.string.SSD_sort_auto));
    items.add(getString(R.string.SSD_sort_start));
    items.add(getString(R.string.SSD_sort_due));
    items.add(getString(R.string.SSD_sort_importance));
    items.add(getString(R.string.SSD_sort_alpha));
    items.add(getString(R.string.SSD_sort_modified));
    items.add(getString(R.string.sort_created));
    items.add(getString(R.string.sort_list));

    if (manualEnabled) {
      if (preferences.isManualSort()) {
        selectedIndex = 0;
      }
    } else if (astridEnabled) {
      if (preferences.isAstridSort()) {
        selectedIndex = 0;
      }
    } else {
      selectedIndex -= 1;
    }

    alertDialog =
        dialogBuilder
            .newDialog()
            .setSingleChoiceItems(
                items,
                selectedIndex,
                (dialog, which) -> {
                  selectedIndex = which;
                  enableReverse();
                })
            .setPositiveButton(R.string.TLA_menu_sort, (dialog, which) -> setSelection(false))
            .setNeutralButton(R.string.reverse, (dialog, which) -> setSelection(true))
            .setNegativeButton(R.string.cancel, null)
            .show();

    enableReverse();

    return alertDialog;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    if (getTargetFragment() == null) {
      callback = (SortDialogCallback) activity;
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt(EXTRA_SELECTED_INDEX, selectedIndex);
  }

  private void enableReverse() {
    if (manualEnabled) {
      Button reverse = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
      reverse.setEnabled(selectedIndex != 0);
    }
  }

  private void setSelection(boolean reverse) {
    preferences.setReverseSort(reverse);

    boolean wasManual = preferences.isManualSort();
    boolean wasAstrid = preferences.isAstridSort();
    boolean isManual = manualEnabled && selectedIndex == 0;
    boolean isAstrid = astridEnabled && selectedIndex == 0;
    preferences.setManualSort(isManual);
    preferences.setAstridSort(isAstrid);

    if (!isManual && !isAstrid) {
      preferences.setSortMode(getSortMode(manualEnabled || astridEnabled ? selectedIndex : selectedIndex + 1));
    }

    Fragment targetFragment = getTargetFragment();
    if (targetFragment == null) {
      callback.sortChanged(wasManual != isManual || wasAstrid != isAstrid);
    } else {
      targetFragment.onActivityResult(getTargetRequestCode(), RESULT_OK, null);
    }
  }

  private int getIndex(int sortMode) {
    switch (sortMode) {
      case SortHelper.SORT_AUTO:
        return 1;
      case SortHelper.SORT_START:
        return 2;
      case SortHelper.SORT_DUE:
        return 3;
      case SortHelper.SORT_IMPORTANCE:
        return 4;
      case SortHelper.SORT_ALPHA:
        return 5;
      case SortHelper.SORT_MODIFIED:
        return 6;
      case SortHelper.SORT_CREATED:
        return 7;
      case SortHelper.SORT_LIST:
        return 8;
    }

    Timber.e("Invalid sort mode: %s", sortMode);
    return 1;
  }

  private int getSortMode(int index) {
    switch (index) {
      case 1:
        return SortHelper.SORT_AUTO;
      case 2:
        return SortHelper.SORT_START;
      case 3:
        return SortHelper.SORT_DUE;
      case 4:
        return SortHelper.SORT_IMPORTANCE;
      case 5:
        return SortHelper.SORT_ALPHA;
      case 6:
        return SortHelper.SORT_MODIFIED;
      case 7:
        return SortHelper.SORT_CREATED;
      case 8:
        return SortHelper.SORT_LIST;
    }

    Timber.e("Invalid sort mode: %s", index);
    return SortHelper.SORT_ALPHA;
  }

  public interface SortDialogCallback {
    void sortChanged(boolean reload);
  }
}
