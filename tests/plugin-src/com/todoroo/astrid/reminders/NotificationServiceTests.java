package com.todoroo.astrid.reminders;

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

import com.todoroo.astrid.R;
import com.todoroo.astrid.reminders.service.NotificationService;

public class NotificationServiceTests extends AndroidTestCase {

    /**
     * Test quiet hour determination logic
     */
    public void testQuietHoursWrapped() {
        Context context = getContext();

        // test wrapped quiet hours
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Editor editor = prefs.edit();
        Resources r = getContext().getResources();

        editor.putString(r.getString(R.string.rmd_EPr_quiet_hours_start_key),
                Integer.toString(22));
        editor.putString(r.getString(R.string.rmd_EPr_quiet_hours_end_key),
                Integer.toString(8));
        editor.commit();

        Date date = new Date();
        date.setHours(21);
        date.setMinutes(59);
        assertFalse(NotificationService.isInQuietHours(context, date));

        date.setHours(22);
        date.setMinutes(0);
        assertTrue(NotificationService.isInQuietHours(context, date));

        date.setHours(23);
        assertTrue(NotificationService.isInQuietHours(context, date));

        date.setHours(0);
        assertTrue(NotificationService.isInQuietHours(context, date));

        date.setHours(7);
        date.setMinutes(59);
        assertTrue(NotificationService.isInQuietHours(context, date));

        date.setHours(8);
        date.setMinutes(0);
        assertFalse(NotificationService.isInQuietHours(context, date));

        date.setHours(12);
        assertFalse(NotificationService.isInQuietHours(context, date));

        date.setHours(20);
        assertFalse(NotificationService.isInQuietHours(context, date));
    }

    /**
     * Test quiet hour determination logic
     */
    public void testQuietHoursUnwrapped() {
        Context context = getContext();

        // test wrapped quiet hours
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Editor editor = prefs.edit();
        Resources r = getContext().getResources();

        editor.putString(r.getString(R.string.rmd_EPr_quiet_hours_start_key),
                Integer.toString(10));
        editor.putString(r.getString(R.string.rmd_EPr_quiet_hours_end_key),
                Integer.toString(16));
        editor.commit();

        Date date = new Date();
        date.setHours(9);
        date.setMinutes(59);
        assertFalse(NotificationService.isInQuietHours(context, date));

        date.setHours(10);
        date.setMinutes(0);
        assertTrue(NotificationService.isInQuietHours(context, date));

        date.setHours(11);
        assertTrue(NotificationService.isInQuietHours(context, date));

        date.setHours(13);
        assertTrue(NotificationService.isInQuietHours(context, date));

        date.setHours(15);
        date.setMinutes(59);
        assertTrue(NotificationService.isInQuietHours(context, date));

        date.setHours(16);
        date.setMinutes(0);
        assertFalse(NotificationService.isInQuietHours(context, date));

        date.setHours(23);
        assertFalse(NotificationService.isInQuietHours(context, date));

        date.setHours(0);
        assertFalse(NotificationService.isInQuietHours(context, date));
    }
}
