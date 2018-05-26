/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.os.Handler;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.todoroo.andlib.utility.AndroidUtilities;
import org.tasks.R;
import timber.log.Timber;

public class NumberPicker extends LinearLayout
    implements OnClickListener, OnFocusChangeListener, OnLongClickListener {

  private static final char[] DIGIT_CHARACTERS =
      new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
  private final Handler mHandler;
  private final EditText mText;
  private final InputFilter mNumberInputFilter;
  private final NumberPickerButton mIncrementButton;
  private final NumberPickerButton mDecrementButton;
  private int incrementBy = 1;
  private int mStart;
  private int mEnd;
  private int mCurrent;
  private OnChangedListener mListener;
  private Formatter mFormatter;
  private boolean mIncrement;
  private boolean mDecrement;
  private final Runnable mRunnable =
      new Runnable() {
        @Override
        public void run() {
          long speed = 60;
          if (mIncrement) {
            changeCurrent(mCurrent + incrementBy);
            mHandler.postDelayed(this, speed);
          } else if (mDecrement) {
            changeCurrent(mCurrent - incrementBy);
            mHandler.postDelayed(this, speed);
          }
        }
      };

  public NumberPicker(Context context) {
    this(context, null);
  }

  public NumberPicker(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
    LayoutInflater inflater =
        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(getLayout(), this, true);
    mHandler = new Handler();
    InputFilter inputFilter = new NumberPickerInputFilter();
    mNumberInputFilter = new NumberRangeKeyListener();
    mIncrementButton = findViewById(R.id.increment);
    mIncrementButton.setOnClickListener(this);
    mIncrementButton.setOnLongClickListener(this);
    mIncrementButton.setNumberPicker(this);
    mDecrementButton = findViewById(R.id.decrement);
    mDecrementButton.setOnClickListener(this);
    mDecrementButton.setOnLongClickListener(this);
    mDecrementButton.setNumberPicker(this);

    mText = findViewById(R.id.timepicker_input);
    mText.setOnFocusChangeListener(this);
    mText.setFilters(new InputFilter[] {inputFilter});

    // disable keyboard until user requests it
    AndroidUtilities.suppressVirtualKeyboard(mText);

    Animation slideUpOutAnimation =
        new TranslateAnimation(
            Animation.RELATIVE_TO_SELF,
            0,
            Animation.RELATIVE_TO_SELF,
            0,
            Animation.RELATIVE_TO_SELF,
            0,
            Animation.RELATIVE_TO_SELF,
            -100);
    slideUpOutAnimation.setDuration(200);
    Animation slideUpInAnimation =
        new TranslateAnimation(
            Animation.RELATIVE_TO_SELF,
            0,
            Animation.RELATIVE_TO_SELF,
            0,
            Animation.RELATIVE_TO_SELF,
            100,
            Animation.RELATIVE_TO_SELF,
            0);
    slideUpInAnimation.setDuration(200);
    Animation slideDownOutAnimation =
        new TranslateAnimation(
            Animation.RELATIVE_TO_SELF,
            0,
            Animation.RELATIVE_TO_SELF,
            0,
            Animation.RELATIVE_TO_SELF,
            0,
            Animation.RELATIVE_TO_SELF,
            100);
    slideDownOutAnimation.setDuration(200);
    Animation slideDownInAnimation =
        new TranslateAnimation(
            Animation.RELATIVE_TO_SELF,
            0,
            Animation.RELATIVE_TO_SELF,
            0,
            Animation.RELATIVE_TO_SELF,
            -100,
            Animation.RELATIVE_TO_SELF,
            0);
    slideDownInAnimation.setDuration(200);

    if (!isEnabled()) {
      setEnabled(false);
    }
  }

  public void setIncrementBy(int incrementBy) {
    this.incrementBy = incrementBy;
  }

  private int getLayout() {
    return R.layout.number_picker;
  }

  /**
   * @return The number of allowable digits that can be typed in (-1 for unlimited) e.g. return 2 if
   *     you don't want to allow 00002 even if 2 is in range.
   */
  private int getMaxDigits() {
    return -1;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    mIncrementButton.setEnabled(enabled);
    mDecrementButton.setEnabled(enabled);
    mText.setEnabled(enabled);
  }

  public void setOnChangeListener(OnChangedListener listener) {
    mListener = listener;
  }

  public void setFormatter(Formatter formatter) {
    mFormatter = formatter;
    updateView();
  }

  /**
   * Set the range of numbers allowed for the number picker. The current value will be automatically
   * set to the start.
   *
   * @param start the start of the range (inclusive)
   * @param end the end of the range (inclusive)
   */
  public void setRange(int start, int end) {
    mStart = start;
    mEnd = end;
    mCurrent = start;
    updateView();
  }

  @Override
  public void onClick(View v) {

    /*
     * The text view may still have focus so clear it's focus which will
     * trigger the on focus changed and any typed values to be pulled.
     */
    mText.clearFocus();

    // now perform the increment/decrement
    if (R.id.increment == v.getId()) {
      changeCurrent(mCurrent + incrementBy);
    } else if (R.id.decrement == v.getId()) {
      changeCurrent(mCurrent - incrementBy);
    }
  }

  private String formatNumber(int value) {
    return (mFormatter != null) ? mFormatter.toString(value) : String.valueOf(value);
  }

  private void changeCurrent(int current) {
    current = notifyChange(current);

    // Wrap around the values if we go past the start or end
    if (current > mEnd) {
      current = mStart + (current - mEnd) - 1;
    } else if (current < mStart) {
      current = mEnd - (mStart - current) + 1;
    }
    mCurrent = current;
    updateView();
  }

  private int notifyChange(int current) {
    if (mListener != null) {
      return mListener.onChanged(current);
    } else {
      return current;
    }
  }

  private void updateView() {

    /*
     * If we don't have displayed values then use the current number else
     * find the correct value in the displayed values for the current
     * number.
     */
    mText.setText(formatNumber(mCurrent));
  }

  private void validateCurrentView(CharSequence str, boolean notifyChange) {
    if (!TextUtils.isEmpty(str)) {
      int val = getSelectedPos(str.toString());
      if ((val >= mStart) && (val <= mEnd)) {
        mCurrent = val;
        if (notifyChange) {
          notifyChange(mCurrent);
        }
      }
    }
    updateView();
  }

  @Override
  public void onFocusChange(View v, boolean hasFocus) {

    /*
     * When focus is lost check that the text field has valid values.
     */
    if (!hasFocus && v instanceof TextView) {
      String str = String.valueOf(((TextView) v).getText());
      if ("".equals(str)) {

        // Restore to the old value as we don't allow empty values
        updateView();
      } else {

        // Check the new value and ensure it's in range
        validateCurrentView(str, true);
      }
    }
  }

  /**
   * We start the long click here but rely on the {@link NumberPickerButton} to inform us when the
   * long click has ended.
   */
  @Override
  public boolean onLongClick(View v) {

    /*
     * The text view may still have focus so clear it's focus which will
     * trigger the on focus changed and any typed values to be pulled.
     */
    mText.clearFocus();

    if (R.id.increment == v.getId()) {
      mIncrement = true;
      mHandler.post(mRunnable);
    } else if (R.id.decrement == v.getId()) {
      mDecrement = true;
      mHandler.post(mRunnable);
    }
    return true;
  }

  public void cancelIncrement() {
    mIncrement = false;
  }

  public void cancelDecrement() {
    mDecrement = false;
  }

  private int getSelectedPos(String str) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException e) {
      Timber.e(e);
      return mStart;
    }
  }

  /** @return the current value. */
  public int getCurrent() {
    String str = String.valueOf(((TextView) mText).getText());
    validateCurrentView(str, true);
    return mCurrent;
  }

  public void setCurrent(int current) {
    mCurrent = current;
    updateView();
  }

  public interface OnChangedListener {

    /** return new value */
    int onChanged(int newVal);
  }

  public interface Formatter {

    String toString(int value);
  }

  private class NumberPickerInputFilter implements InputFilter {

    @Override
    public CharSequence filter(
        CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
      return mNumberInputFilter.filter(source, start, end, dest, dstart, dend);
    }
  }

  private class NumberRangeKeyListener extends NumberKeyListener {

    @Override
    protected char[] getAcceptedChars() {
      return DIGIT_CHARACTERS;
    }

    @Override
    public CharSequence filter(
        CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

      CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
      if (filtered == null) {
        filtered = source.subSequence(start, end);
      }

      String result =
          String.valueOf(dest.subSequence(0, dstart))
              + filtered
              + dest.subSequence(dend, dest.length());

      if ("".equals(result)) {
        return result;
      }

      if (getMaxDigits() > 0 && result.length() > getMaxDigits()) {
        return "";
      }

      int val = getSelectedPos(result);

      /*
       * Ensure the user can't type in a value greater than the max
       * allowed. We have to allow less than min as the user might want to
       * delete some numbers and then type a new number.
       */
      if (val > mEnd) {
        return "";
      } else {
        return filtered;
      }
    }

    @Override
    public int getInputType() {
      return 0;
    }
  }
}
