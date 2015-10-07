package org.tasks;

import android.content.Context;

public class TestUtilities {
    private static boolean mockitoInitialized;

    public static void initializeMockito(Context context) {
        if (!mockitoInitialized) {
            // for mockito: https://code.google.com/p/dexmaker/issues/detail?id=2
            System.setProperty("dexmaker.dexcache", context.getCacheDir().toString());
            mockitoInitialized = true;
        }
    }
}
