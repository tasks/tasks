package com.todoroo.astrid.utility;

public class Flags {

    private static int state = 0;

    /**
     * Whether to refresh the task list when displaying it. If you are
     * writing a background service, send a BROADCAST_EVENT_REFRESH
     * instead, as this is only checked periodically and when loading task list.
     */
    public static final int REFRESH = 1 << 0;

    /**
     * If set, indicates tags changed during task save
     */
    public static final int TAGS_CHANGED = 1 << 1;

    /**
     * If set, sync service should toast on save or failure
     */
    public static final int TOAST_ON_SAVE = 1 << 2;

    /**
     * If set, indicates to suppress the next act.fm sync attempt
     */
    public static final int ACTFM_SUPPRESS_SYNC = 1 << 3;

    /**
     * If set, indicates save hooks should be suppressed
     */
    public static final int SUPPRESS_HOOKS = 1 << 4;

    /**
     * If set, indicates to suppress the next gtasks sync attempt
     */

    public static final int GTASKS_SUPPRESS_SYNC = 1 << 5;

    public static boolean checkAndClear(int flag) {
        boolean set = (state & flag) > 0;
        state &= ~flag;
        return set;
    }

    public static boolean check(int flag) {
        return (state & flag) > 0;
    }

    public static void set(int flag) {
        state |= flag;
    }

}
