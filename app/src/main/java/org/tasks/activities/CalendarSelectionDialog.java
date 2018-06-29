package org.tasks.activities;

import static com.google.common.collect.Lists.transform;
import static org.tasks.PermissionUtil.verifyPermissions;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.ListView;
import android.widget.Toast;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.calendars.AndroidCalendar;
import org.tasks.calendars.CalendarProvider;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.FragmentPermissionRequestor;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.themes.Theme;
import org.tasks.ui.SingleCheckedArrayAdapter;

public class CalendarSelectionDialog extends InjectingDialogFragment {

  private static final String EXTRA_SELECTED = "extra_selected";
  private final List<String> calendarNames = new ArrayList<>();
  private final List<AndroidCalendar> calendars = new ArrayList<>();
  @Inject DialogBuilder dialogBuilder;
  @Inject CalendarProvider calendarProvider;
  @Inject PermissionChecker permissionChecker;
  @Inject FragmentPermissionRequestor permissionRequestor;
  @Inject Theme theme;
  private CalendarSelectionHandler handler;
  private SingleCheckedArrayAdapter adapter;
  private ListView listView;

  public static CalendarSelectionDialog newCalendarSelectionDialog(String selected) {
    CalendarSelectionDialog dialog = new CalendarSelectionDialog();
    Bundle arguments = new Bundle();
    arguments.putString(EXTRA_SELECTED, selected);
    dialog.setArguments(arguments);
    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    theme.applyToContext(getActivity());

    adapter =
        new SingleCheckedArrayAdapter(getActivity(), calendarNames, theme.getThemeAccent()) {
          @Override
          protected int getDrawable(int position) {
            return R.drawable.ic_event_24dp;
          }

          @Override
          protected int getDrawableColor(int position) {
            return calendars.get(position).getColor();
          }
        };

    AlertDialog dialog = dialogBuilder
        .newDialog()
        .setSingleChoiceItems(
            adapter, -1, (d, which) -> handler.selectedCalendar(calendars.get(which)))
        .setOnDismissListener(dialogInterface -> handler.cancel())
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
      handler.cancel();
    } else {
      calendars.add(0, new AndroidCalendar(null, getString(R.string.dont_add_to_calendar), -1));
      calendarNames.addAll(transform(calendars, AndroidCalendar::getName));
      Bundle arguments = getArguments();
      String selected = arguments.getString(EXTRA_SELECTED);
      int selectedIndex = Strings.isNullOrEmpty(selected) ? 0 : calendarNames.indexOf(selected);
      adapter.notifyDataSetChanged();
      listView.setItemChecked(selectedIndex, true);
      listView.setSelection(selectedIndex);
    }
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    handler = (CalendarSelectionHandler) activity;
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    super.onCancel(dialog);

    handler.cancel();
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PermissionRequestor.REQUEST_CALENDAR) {
      if (verifyPermissions(grantResults)) {
        loadCalendars();
      } else {
        handler.cancel();
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }

  public interface CalendarSelectionHandler {

    void selectedCalendar(AndroidCalendar calendar);

    void cancel();
  }
}
