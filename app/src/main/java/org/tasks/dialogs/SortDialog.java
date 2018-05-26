package org.tasks.dialogs;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import com.todoroo.astrid.core.SortHelper;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class SortDialog extends InjectingDialogFragment {

  private static final String EXTRA_MANUAL_ENABLED = "extra_manual_enabled";
  private static final String EXTRA_SELECTED_INDEX = "extra_selected_index";
  @Inject Preferences preferences;
  @Inject DialogBuilder dialogBuilder;
  private boolean manualEnabled;
  private int selectedIndex;
  private AlertDialog alertDialog;
  private SortDialogCallback callback;

  public static SortDialog newSortDialog(boolean manualEnabled) {
    SortDialog sortDialog = new SortDialog();
    sortDialog.manualEnabled = manualEnabled;
    return sortDialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      manualEnabled = savedInstanceState.getBoolean(EXTRA_MANUAL_ENABLED);
      selectedIndex = savedInstanceState.getInt(EXTRA_SELECTED_INDEX);
    } else {
      selectedIndex = getIndex(preferences.getSortMode());
    }

    List<String> items = new ArrayList<>();

    if (manualEnabled) {
      items.add(getString(R.string.SSD_sort_drag));
    }

    items.add(getString(R.string.SSD_sort_auto));
    items.add(getString(R.string.SSD_sort_due));
    items.add(getString(R.string.SSD_sort_importance));
    items.add(getString(R.string.SSD_sort_alpha));
    items.add(getString(R.string.SSD_sort_modified));

    if (manualEnabled) {
      if (preferences.getBoolean(R.string.p_manual_sort, false)) {
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
            .setNegativeButton(android.R.string.cancel, null)
            .showThemedListView();

    enableReverse();

    return alertDialog;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    callback = (SortDialogCallback) activity;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putBoolean(EXTRA_MANUAL_ENABLED, manualEnabled);
    outState.putInt(EXTRA_SELECTED_INDEX, selectedIndex);
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

    callback.sortChanged();
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

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }

  public interface SortDialogCallback {

    void sortChanged();
  }
}
