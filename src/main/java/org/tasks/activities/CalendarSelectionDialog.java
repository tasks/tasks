package org.tasks.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.common.base.Function;

import org.tasks.R;
import org.tasks.calendars.AndroidCalendar;
import org.tasks.calendars.CalendarProvider;
import org.tasks.dialogs.AlertDialogBuilder;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.preferences.FragmentPermissionRequestor;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.PermissionRequestor;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.google.common.collect.Lists.transform;
import static org.tasks.PermissionUtil.verifyPermissions;

public class CalendarSelectionDialog extends InjectingDialogFragment {

    public interface CalendarSelectionHandler {
        void selectedCalendar(AndroidCalendar calendar);

        void cancel();
    }

    @Inject DialogBuilder dialogBuilder;
    @Inject CalendarProvider calendarProvider;
    @Inject @ForActivity Context context;
    @Inject FragmentPermissionRequestor fragmentPermissionRequestor;
    @Inject PermissionChecker permissionChecker;
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
        }

        adapter = new ArrayAdapter<>(context, R.layout.simple_list_item_single_choice_themed, calendarNames);

        AlertDialogBuilder builder = dialogBuilder.newDialog()
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.selectedCalendar(calendars.get(which));
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        handler.cancel();
                    }
                });
        if (enableNone) {
            builder.setNeutralButton(R.string.none, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.selectedCalendar(AndroidCalendar.NONE);
                }
            });
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
            calendarNames.addAll(transform(calendars, new Function<AndroidCalendar, String>() {
                @Override
                public String apply(AndroidCalendar androidCalendar) {
                    return androidCalendar.getName();
                }
            }));
            if (calendarNames.isEmpty()) {
                Toast.makeText(context, R.string.no_calendars_found, Toast.LENGTH_LONG).show();
                handler.cancel();
            } else {
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
            if (grantResults.length > 0 && !verifyPermissions(grantResults)) {
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

    public void enableNone() {
        enableNone = true;
    }

    public void setCalendarSelectionHandler(CalendarSelectionHandler handler) {
        this.handler = handler;
    }
}
