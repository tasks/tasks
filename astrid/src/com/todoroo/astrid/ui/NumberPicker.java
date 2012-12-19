/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
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

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.AndroidUtilities;

@SuppressWarnings({"nls","unused"})
public class NumberPicker extends LinearLayout implements OnClickListener,
        OnFocusChangeListener, OnLongClickListener {

    public interface OnChangedListener {
        /** return new value */
        int onChanged(NumberPicker picker, int oldVal, int newVal);
    }

    public interface Formatter {
        String toString(int value);
    }

    /*
     * Use a custom NumberPicker formatting callback to use two-digit minutes
     * strings like "01". Keeping a static formatter etc. is the most efficient
     * way to do this; it avoids creating temporary objects on every call to
     * format().
     */
    public static final NumberPicker.Formatter TWO_DIGIT_FORMATTER = new NumberPicker.Formatter() {
        final StringBuilder       mBuilder = new StringBuilder();
        final java.util.Formatter mFmt     = new java.util.Formatter(mBuilder);
        final Object[]            mArgs    = new Object[1];

        public String toString(int value) {
            mArgs[0] = value;
            mBuilder.delete(0, mBuilder.length());
            mFmt.format("%02d", mArgs);
            return mFmt.toString();
        }
    };

    private int incrementBy = 1;
    public void setIncrementBy(int incrementBy) {
        this.incrementBy = incrementBy;
    }

    private final Handler  mHandler;
    private final Runnable mRunnable = new Runnable() {
        public void run() {
            if (mIncrement) {
                changeCurrent(mCurrent + incrementBy, mSlideUpInAnimation, mSlideUpOutAnimation);
                mHandler.postDelayed(this, mSpeed);
            } else if (mDecrement) {
                changeCurrent(mCurrent - incrementBy, mSlideDownInAnimation, mSlideDownOutAnimation);
                mHandler.postDelayed(this, mSpeed);
            }
        }
    };

    private final LayoutInflater               mInflater;
    private final EditText                     mText;
    private final InputFilter                  mInputFilter;
    private final InputFilter                  mNumberInputFilter;

    private final Animation                    mSlideUpOutAnimation;
    private final Animation                    mSlideUpInAnimation;
    private final Animation                    mSlideDownOutAnimation;
    private final Animation                    mSlideDownInAnimation;

    private String[]                           mDisplayedValues;
    private int                                mStart;
    private int                                mEnd;
    private int                                mCurrent;
    private int                                mPrevious;
    private OnChangedListener                  mListener;
    private Formatter                          mFormatter;
    private long                               mSpeed              = 60;

    private boolean                            mIncrement;
    private boolean                            mDecrement;

    public NumberPicker(Context context) {
        this(context, null);
    }

    public NumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    protected int getLayout() {
        return R.layout.number_picker;
    }

    /**
     * @return The number of allowable digits that can be typed in (-1 for unlimited)
     * e.g. return 2 if you don't want to allow 00002 even if 2 is in range.
     */
    protected int getMaxDigits() {
        return -1;
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        setOrientation(VERTICAL);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater.inflate(getLayout(), this, true);
        mHandler = new Handler();
        mInputFilter = new NumberPickerInputFilter();
        mNumberInputFilter = new NumberRangeKeyListener();
        mIncrementButton = (NumberPickerButton) findViewById(R.id.increment);
        mIncrementButton.setOnClickListener(this);
        mIncrementButton.setOnLongClickListener(this);
        mIncrementButton.setNumberPicker(this);
        mDecrementButton = (NumberPickerButton) findViewById(R.id.decrement);
        mDecrementButton.setOnClickListener(this);
        mDecrementButton.setOnLongClickListener(this);
        mDecrementButton.setNumberPicker(this);

        mText = (EditText) findViewById(R.id.timepicker_input);
        mText.setOnFocusChangeListener(this);
        mText.setFilters(new InputFilter[] { mInputFilter });

        // disable keyboard until user requests it
        AndroidUtilities.suppressVirtualKeyboard(mText);

        mSlideUpOutAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -100);
        mSlideUpOutAnimation.setDuration(200);
        mSlideUpInAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 100, Animation.RELATIVE_TO_SELF, 0);
        mSlideUpInAnimation.setDuration(200);
        mSlideDownOutAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 100);
        mSlideDownOutAnimation.setDuration(200);
        mSlideDownInAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, -100, Animation.RELATIVE_TO_SELF, 0);
        mSlideDownInAnimation.setDuration(200);

        if (!isEnabled()) {
            setEnabled(false);
        }
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
     * Set the range of numbers allowed for the number picker. The current value
     * will be automatically set to the start.
     *
     * @param start
     *            the start of the range (inclusive)
     * @param end
     *            the end of the range (inclusive)
     */
    public void setRange(int start, int end) {
        mStart = start;
        mEnd = end;
        mCurrent = start;
        updateView();
    }

    /**
     * Set the range of numbers allowed for the number picker. The current value
     * will be automatically set to the start. Also provide a mapping for values
     * used to display to the user.
     *
     * @param start
     *            the start of the range (inclusive)
     * @param end
     *            the end of the range (inclusive)
     * @param displayedValues
     *            the values displayed to the user.
     */
    public void setRange(int start, int end, String[] displayedValues) {
        mDisplayedValues = displayedValues;
        mStart = start;
        mEnd = end;
        mCurrent = start;
        updateView();
    }

    public void setCurrent(int current) {
        mCurrent = current;
        updateView();
    }

    /**
     * The speed (in milliseconds) at which the numbers will scroll when the the
     * +/- buttons are longpressed. Default is 300ms.
     */
    public void setSpeed(long speed) {
        mSpeed = speed;
    }

    public void onClick(View v) {

        /*
         * The text view may still have focus so clear it's focus which will
         * trigger the on focus changed and any typed values to be pulled.
         */
        mText.clearFocus();

        // now perform the increment/decrement
        if (R.id.increment == v.getId()) {
            changeCurrent(mCurrent + incrementBy, mSlideUpInAnimation,
                    mSlideUpOutAnimation);
        } else if (R.id.decrement == v.getId()) {
            changeCurrent(mCurrent - incrementBy, mSlideDownInAnimation,
                    mSlideDownOutAnimation);
        }
    }

    private String formatNumber(int value) {
        return (mFormatter != null) ? mFormatter.toString(value) : String
                .valueOf(value);
    }

    private void changeCurrent(int current, Animation in, Animation out) {
        current = notifyChange(current);

        // Wrap around the values if we go past the start or end
        if (current > mEnd) {;
            current = mStart + (current - mEnd) - 1;
        } else if (current < mStart) {
            current = mEnd - (mStart - current) + 1;
        }
        mPrevious = mCurrent;
        mCurrent = current;
        updateView();
    }

    private int notifyChange(int current) {
        if (mListener != null) {
            return mListener.onChanged(this, mCurrent, current);
        } else
            return current;

    }

    public void updateView() {

        /*
         * If we don't have displayed values then use the current number else
         * find the correct value in the displayed values for the current
         * number.
         */
        if (mDisplayedValues == null) {
            mText.setText(formatNumber(mCurrent));
        } else {
            mText.setText(mDisplayedValues[mCurrent - mStart]);
        }
    }

    public void validateAndUpdate() {
        String str = String.valueOf(mText.getText());
        if (TextUtils.isEmpty(str)) {
            updateView();
        } else {
            validateCurrentView(str, false);
        }
    }

    private void validateCurrentView(CharSequence str, boolean notifyChange) {
        if (!TextUtils.isEmpty(str)) {
            int val = getSelectedPos(str.toString());
            if ((val >= mStart) && (val <= mEnd)) {
                mPrevious = mCurrent;
                mCurrent = val;
                if (notifyChange)
                    notifyChange(mCurrent);
            }
        }
        updateView();
    }

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
     * We start the long click here but rely on the {@link NumberPickerButton}
     * to inform us when the long click has ended.
     */
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

    private static final char[] DIGIT_CHARACTERS = new char[] { '0', '1', '2',
        '3', '4', '5', '6', '7', '8', '9'       };

    private final NumberPickerButton  mIncrementButton;
    private final NumberPickerButton  mDecrementButton;

    class NumberPickerInputFilter implements InputFilter {
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            if (mDisplayedValues == null) {
                return mNumberInputFilter.filter(source, start, end, dest,
                        dstart, dend);
            }
            CharSequence filtered = String.valueOf(source.subSequence(start,
                    end));
            String result = String.valueOf(dest.subSequence(0, dstart))
                + filtered + dest.subSequence(dend, dest.length());
            String str = String.valueOf(result).toLowerCase();
            for (String val : mDisplayedValues) {
                val = val.toLowerCase();
                if (val.startsWith(str)) {
                    return filtered;
                }
            }
            return "";
        }
    }

    class NumberRangeKeyListener extends NumberKeyListener {

        @Override
        protected char[] getAcceptedChars() {
            return DIGIT_CHARACTERS;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {

            CharSequence filtered = super.filter(source, start, end, dest,
                    dstart, dend);
            if (filtered == null) {
                filtered = source.subSequence(start, end);
            }

            String result = String.valueOf(dest.subSequence(0, dstart))
                + filtered + dest.subSequence(dend, dest.length());

            if ("".equals(result)) {
                return result;
            }

            if (getMaxDigits() > 0 && result.length() > getMaxDigits())
                return "";

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

		public int getInputType() {
			return 0;
		}
    }

    private int getSelectedPos(String str) {
        if (mDisplayedValues == null) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return mStart;
            }
        } else {
            for (int i = 0; i < mDisplayedValues.length; i++) {

                /* Don't force the user to type in jan when ja will do */
                str = str.toLowerCase();
                if (mDisplayedValues[i].toLowerCase().startsWith(str)) {
                    return mStart + i;
                }
            }

            /*
             * The user might have typed in a number into the month field i.e.
             * 10 instead of OCT so support that too.
             */
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {

                /* Ignore as if it's not a number we don't care */
            }
        }
        return mStart;
    }

    /**
     * Override the number picker's text
     * @param text
     */
    public void setText(String text) {
        mText.setText(text);
    }

    /**
     * @return the current value.
     */
    public int getCurrent() {
        String str = String.valueOf(((TextView) mText).getText());
        validateCurrentView(str, true);
        return mCurrent;
    }
}
