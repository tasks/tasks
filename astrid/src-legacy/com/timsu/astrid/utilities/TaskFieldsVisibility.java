package com.timsu.astrid.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

/** Helper struct to store preferences for field visibility */
public class TaskFieldsVisibility {

    // --- they're like constants... except mutable =/

    public boolean TITLE;
    public boolean TIMES;
    public boolean IMPORTANCE;
    public boolean DEADLINE;
    public boolean REMINDERS;
    public boolean REPEATS;
    public boolean TAGS;
    public boolean NOTES;

    private static class PrefReader {
        SharedPreferences prefs;
        Resources r;

        public PrefReader(SharedPreferences prefs, Resources r) {
            this.prefs = prefs;
            this.r = r;
        }

        private boolean get(int key, int defValue) {
            return prefs.getBoolean(r.getString(key),
                    Boolean.parseBoolean(r.getString(defValue)));
        }
    }

    public static TaskFieldsVisibility getFromPreferences(Context context,
            SharedPreferences prefs) {
        TaskFieldsVisibility tf = new TaskFieldsVisibility();
        Resources r = context.getResources();
        PrefReader pr = new PrefReader(prefs, r);

        tf.TITLE       = true;
        tf.TIMES       = true;
        tf.IMPORTANCE  = true;
        tf.DEADLINE    = true;
        tf.REMINDERS   = true;
        tf.REPEATS     = true;
        tf.TAGS        = true;
        tf.NOTES       = true;

        return tf;
    }
}
