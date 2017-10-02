package org.tasks.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.common.base.Strings;

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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Lists.transform;
import static org.tasks.PermissionUtil.verifyPermissions;

public class CalendarSelectionDialog extends InjectingDialogFragment {

    public static CalendarSelectionDialog newCalendarSelectionDialog(String selected) {
        CalendarSelectionDialog dialog = new CalendarSelectionDialog();
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_SELECTED, selected);
        dialog.setArguments(arguments);
        return dialog;
    }

    public interface CalendarSelectionHandler {
        void selectedCalendar(AndroidCalendar calendar);

        void cancel();
    }

    private static final String EXTRA_SELECTED = "extra_selected";

    @Inject DialogBuilder dialogBuilder;
    @Inject CalendarProvider calendarProvider;
    @Inject FragmentPermissionRequestor fragmentPermissionRequestor;
    @Inject PermissionChecker permissionChecker;
    @Inject Theme theme;

    private CalendarSelectionHandler handler;
    private String selected;
    private SingleCheckedArrayAdapter adapter;
    private final List<AndroidCalendar> calendars = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        fragmentPermissionRequestor.requestCalendarPermissions();

        Bundle arguments = getArguments();
        selected = arguments.getString(EXTRA_SELECTED);

        theme.applyToContext(getActivity());
        adapter = new SingleCheckedArrayAdapter(getActivity(), transform(calendars, AndroidCalendar::getName), theme.getThemeAccent()) {
            @Override
            protected int getDrawable(int position) {
                return R.drawable.ic_event_24dp;
            }

            @Override
            protected int getDrawableColor(int position) {
                return calendars.get(position).getColor();
            }
        };

        return dialogBuilder.newDialog()
                .setSingleChoiceItems(adapter, -1, (dialog, which) -> handler.selectedCalendar(calendars.get(which)))
                .setOnDismissListener(dialogInterface -> handler.cancel())
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (permissionChecker.canAccessCalendars()) {
            calendars.clear();
            calendars.addAll(calendarProvider.getCalendars());
            if (calendars.isEmpty()) {
                Toast.makeText(getActivity(), R.string.no_calendars_found, Toast.LENGTH_LONG).show();
                handler.cancel();
            } else {
                calendars.add(0, new AndroidCalendar(null, getString(R.string.dont_add_to_calendar), -1));
                if (Strings.isNullOrEmpty(selected)) {
                    adapter.setChecked(0);
                } else {
                    adapter.setChecked(selected);
                }
                adapter.notifyDataSetChanged();
            }
        }
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
