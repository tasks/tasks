/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import org.tasks.R;

/** This class exists purely to cancel long click events. */
public class NumberPickerButton extends AppCompatImageButton {

  private NumberPicker mNumberPicker;

  public NumberPickerButton(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public NumberPickerButton(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public NumberPickerButton(Context context) {
    super(context);
  }

  public void setNumberPicker(NumberPicker picker) {
    mNumberPicker = picker;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    cancelLongpressIfRequired(event);
    return super.onTouchEvent(event);
  }

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    cancelLongpressIfRequired(event);
    return super.onTrackballEvent(event);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER) || (keyCode == KeyEvent.KEYCODE_ENTER)) {
      cancelLongpress();
    }
    return super.onKeyUp(keyCode, event);
  }

  private void cancelLongpressIfRequired(MotionEvent event) {
    if ((event.getAction() == MotionEvent.ACTION_CANCEL)
        || (event.getAction() == MotionEvent.ACTION_UP)) {
      cancelLongpress();
    }
  }

  private void cancelLongpress() {
    if (R.id.increment == getId()) {
      mNumberPicker.cancelIncrement();
    } else if (R.id.decrement == getId()) {
      mNumberPicker.cancelDecrement();
    }
  }
}
