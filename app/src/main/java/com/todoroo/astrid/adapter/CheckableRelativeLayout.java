package com.todoroo.astrid.adapter;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.RelativeLayout;

public class CheckableRelativeLayout extends RelativeLayout implements Checkable {

  private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};

  private boolean checked = false;

  public CheckableRelativeLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean isChecked() {
    return checked;
  }

  @Override
  public void setChecked(boolean b) {
    if (b != checked) {
      checked = b;
      refreshDrawableState();
    }
  }

  @Override
  public void toggle() {
    setChecked(!checked);
  }

  @Override
  public int[] onCreateDrawableState(int extraSpace) {
    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
    if (isChecked()) {
      mergeDrawableStates(drawableState, CHECKED_STATE_SET);
    }
    return drawableState;
  }
}
