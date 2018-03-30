/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.utility;

public class Flags {

  /** If set, indicates tags changed during task save */
  public static final int TAGS_CHANGED = 1 << 1;
  /** If set, indicates that TaskListFragmentPager should not intercept touch events */
  public static final int TLFP_NO_INTERCEPT_TOUCH = 1 << 7;

  private static int state = 0;

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
