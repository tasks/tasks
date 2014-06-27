package org.tasks;

import android.content.Context;

import java.util.Date;

public class TestUtilities {
    private static boolean mockitoInitialized;

    public static void initializeMockito(Context context) {
        if (!mockitoInitialized) {
            // for mockito: https://code.google.com/p/dexmaker/issues/detail?id=2
            System.setProperty("dexmaker.dexcache", context.getCacheDir().toString());
            mockitoInitialized = true;
        }
    }

    public static Date newDateTime(int year, int month, int day, int hour, int minute, int second) {
        return new Date(year - 1900, month - 1, day, hour, minute, second);
    }
}
