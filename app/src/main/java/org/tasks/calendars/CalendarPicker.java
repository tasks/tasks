package org.tasks.calendars;

import static com.google.common.collect.Lists.transform;
import static org.tasks.PermissionUtil.verifyPermissions;
import static org.tasks.Strings.isNullOrEmpty;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.FragmentPermissionRequestor;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.themes.Theme;
import org.tasks.ui.SingleCheckedArrayAdapter;

public class CalendarPicker extends InjectingDialogFragment {

  public static final String EXTRA_CALENDAR_ID = "extra_calendar_id";
  public static final String EXTRA_CALENDAR_NAME = "extra_calendar_name";
  private static final String EXTRA_SELECTED = "extra_selected";
  private final List<String> calendarNames = new ArrayList<>();
  private final List<AndroidCalendar> calendars = new ArrayList<>();
  @Inject DialogBuilder dialogBuilder;
  @Inject CalendarProvider calendarProvider;
  @Inject PermissionChecker permissionChecker;
  @Inject FragmentPermissionRequestor permissionRequestor;
  @Inject Theme theme;
  private SingleCheckedArrayAdapter adapter;
  private ListView listView;

  public static CalendarPicker newCalendarPicker(Fragment target, int rc, String selected) {
    Bundle arguments = new Bundle();
    arguments.putString(EXTRA_SELECTED, selected);
    CalendarPicker fragment = new CalendarPicker();
    fragment.setArguments(arguments);
    fragment.setTargetFragment(target, rc);
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    theme.applyToContext(getActivity());

    adapter =
        new SingleCheckedArrayAdapter(getActivity(), calendarNames) {
          @Override
          protected int getDrawable() {
            return R.drawable.ic_outline_event_24px;
          }

          @Override
          protected int getDrawableColor(int position) {
            return calendars.get(position).getColor();
          }
        };

    AlertDialog dialog =
        dialogBuilder
            .newDialog()
            .setSingleChoiceItems(
                adapter, -1, (d, which) -> {
                  dismiss();
                  AndroidCalendar calendar = calendars.get(which);
                  Intent data = new Intent();
                  data.putExtra(EXTRA_CALENDAR_ID, calendar.getId());
                  data.putExtra(EXTRA_CALENDAR_NAME, calendar.getName());
                  getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
                })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    listView = dialog.getListView();
    if (permissionChecker.canAccessCalendars()) {
      loadCalendars();
    } else if (savedInstanceState == null) {
      permissionRequestor.requestCalendarPermissions();
    }
    return dialog;
  }

  private void loadCalendars() {
    calendars.clear();
    calendarNames.clear();

    calendars.addAll(calendarProvider.getCalendars());
    if (calendars.isEmpty()) {
      Toast.makeText(getActivity(), R.string.no_calendars_found, Toast.LENGTH_LONG).show();
      dismiss();
    } else {
      calendars.add(0, new AndroidCalendar(null, getString(R.string.dont_add_to_calendar), -1));
      calendarNames.addAll(transform(calendars, AndroidCalendar::getName));
      Bundle arguments = getArguments();
      String selected = arguments.getString(EXTRA_SELECTED);
      int selectedIndex = isNullOrEmpty(selected) ? 0 : calendarNames.indexOf(selected);
      adapter.notifyDataSetChanged();
      listView.setItemChecked(selectedIndex, true);
      listView.setSelection(selectedIndex);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_CALENDAR) {
      if (verifyPermissions(grantResults)) {
        loadCalendars();
      } else {
        dismiss();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }
}
