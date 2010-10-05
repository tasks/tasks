package com.todoroo.astrid.utility;

public class Flags {

    private static int state = 0;

    /**
     * Whether to refresh the task list
     */
    public static final int REFRESH = 1 << 0;

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
