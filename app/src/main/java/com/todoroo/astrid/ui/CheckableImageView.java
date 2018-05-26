/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.ViewDebug;
import android.widget.Checkable;

public class CheckableImageView extends AppCompatImageView implements Checkable {

  private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
  private boolean mChecked;
  private boolean mBroadcasting;

  public CheckableImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean performClick() {
    toggle();
    return super.performClick();
  }

  @Override
  public void toggle() {
    setChecked(!mChecked);
  }

  @Override
  @ViewDebug.ExportedProperty
  public boolean isChecked() {
    return mChecked;
  }

  /**
   * Changes the checked state of this button.
   *
   * @param checked true to check the button, false to uncheck it
   */
  @Override
  public void setChecked(boolean checked) {
    if (mChecked != checked) {
      mChecked = checked;
      refreshDrawableState();

      // Avoid infinite recursions if setChecked() is called from a listener
      if (mBroadcasting) {
        return;
      }

      mBroadcasting = false;
    }
  }

  @Override
  public int[] onCreateDrawableState(int extraSpace) {
    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
    if (isChecked()) {
      mergeDrawableStates(drawableState, CHECKED_STATE_SET);
    }
    return drawableState;
  }

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();

    if (getDrawable() != null) {
      int[] myDrawableState = getDrawableState();

      // Set the state of the Drawable
      getDrawable().setState(myDrawableState);

      invalidate();
    }
  }
}
