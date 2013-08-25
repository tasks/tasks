/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
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
     * If set, indicates that the edit popover was dismissed by the edit fragment/back button
     */
    public static final int TLA_DISMISSED_FROM_TASK_EDIT = 1 << 5;

    /**
     * If set, indicates that task list activity was resumed after voice add (so don't replace refresh list fragment)
     */
    public static final int TLA_RESUMED_FROM_VOICE_ADD = 1 << 6;

    /**
     * If set, indicates that TaskListFragmentPager should not intercept touch events
     */
    public static final int TLFP_NO_INTERCEPT_TOUCH = 1 << 7;

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
