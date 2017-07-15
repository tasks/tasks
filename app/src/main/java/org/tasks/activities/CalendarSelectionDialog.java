package org.tasks.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.tasks.R;
import org.tasks.calendars.AndroidCalendar;
import org.tasks.calendars.CalendarProvider;
import org.tasks.dialogs.AlertDialogBuilder;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.FragmentPermissionRequestor;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;
import org.tasks.themes.Theme;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Lists.transform;
import static org.tasks.PermissionUtil.verifyPermissions;

public class CalendarSelectionDialog extends InjectingDialogFragment {

    public static CalendarSelectionDialog newCalendarSelectionDialog(boolean enableNone) {
        CalendarSelectionDialog dialog = new CalendarSelectionDialog();
        dialog.enableNone = enableNone;
        return dialog;
    }

    public interface CalendarSelectionHandler {
        void selectedCalendar(AndroidCalendar calendar);

        void cancel();
    }

    private static final String EXTRA_NONE_ENABLED = "extra_none_enabled";

    @Inject DialogBuilder dialogBuilder;
    @Inject CalendarProvider calendarProvider;
    @Inject FragmentPermissionRequestor fragmentPermissionRequestor;
    @Inject PermissionChecker permissionChecker;
    @Inject Theme theme;
    private CalendarSelectionHandler handler;
    private boolean enableNone;
    private ArrayAdapter<String> adapter;
    private final List<AndroidCalendar> calendars = new ArrayList<>();
    private final List<String> calendarNames = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            fragmentPermissionRequestor.requestCalendarPermissions();
        } else {
            enableNone = savedInstanceState.getBoolean(EXTRA_NONE_ENABLED);
        }

        theme.applyToContext(getActivity());
        adapter = new ArrayAdapter<>(getActivity(), R.layout.simple_list_item_single_choice_themed, calendarNames);

        AlertDialogBuilder builder = dialogBuilder.newDialog()
                .setAdapter(adapter, (dialog, which) -> handler.selectedCalendar(calendars.get(which)))
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> handler.cancel());
        if (enableNone) {
            builder.setNeutralButton(R.string.none, (dialog, which) -> handler.selectedCalendar(AndroidCalendar.NONE));
        }

        return builder.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (permissionChecker.canAccessCalendars()) {
            calendars.clear();
            calendars.addAll(calendarProvider.getCalendars());
            calendarNames.clear();
            calendarNames.addAll(transform(calendars, AndroidCalendar::getName));
            if (calendarNames.isEmpty()) {
                Toast.makeText(getActivity(), R.string.no_calendars_found, Toast.LENGTH_LONG).show();
                handler.cancel();
            } else {
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(EXTRA_NONE_ENABLED, enableNone);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        handler.cancel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionRequestor.REQUEST_CALENDAR) {
            if (!verifyPermissions(grantResults)) {
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

    public void setCalendarSelectionHandler(CalendarSelectionHandler handler) {
        this.handler = handler;
    }
}
