package org.tasks.ui;

public class DisplayableException extends RuntimeException {

  private final int resId;

  public DisplayableException(int resId) {
    this.resId = resId;
  }

  public int getResId() {
    return resId;
  }
}
