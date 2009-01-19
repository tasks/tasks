package com.timsu.astrid.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.timsu.astrid.R;

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

        tf.TITLE       = pr.get(R.string.prefs_titleVisible, R.string.prefs_titleVisible_default);
        tf.TIMES       = pr.get(R.string.prefs_timeVisible, R.string.prefs_timeVisible_default);
        tf.IMPORTANCE  = pr.get(R.string.prefs_importanceVisible, R.string.prefs_importanceVisible_default);
        tf.DEADLINE    = pr.get(R.string.prefs_deadlineVisible, R.string.prefs_deadlineVisible_default);
        tf.REMINDERS   = pr.get(R.string.prefs_reminderVisible, R.string.prefs_reminderVisible_default);
        tf.REPEATS     = pr.get(R.string.prefs_repeatVisible, R.string.prefs_repeatVisible_default);
        tf.TAGS        = pr.get(R.string.prefs_tagsVisible, R.string.prefs_tagsVisible_default);
        tf.NOTES       = pr.get(R.string.prefs_notesVisible, R.string.prefs_notesVisible_default);

        return tf;
    }
}
