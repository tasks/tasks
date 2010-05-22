/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.todoroo.andlib.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;

public class NumberPickerWidget extends LinearLayout implements OnClickListener,
        OnFocusChangeListener, OnLongClickListener {

    @Autowired
    private Integer numberPickerLayout;

    @Autowired
    private Integer numberPickerIncrementId;

    @Autowired
    private Integer numberPickerDecrementId;

    @Autowired
    private Integer numberPickerInputId;

    public interface OnChangedListener {
        void onChanged(NumberPickerWidget picker, int oldVal, int newVal);
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
    public static final NumberPickerWidget.Formatter TWO_DIGIT_FORMATTER = new NumberPickerWidget.Formatter() {
        final StringBuilder       mBuilder = new StringBuilder();
        final java.util.Formatter mFmt     = new java.util.Formatter(mBuilder);
        final Object[]            mArgs    = new Object[1];

        public String toString(int value) {
            mArgs[0] = value;
            mBuilder.delete(0, mBuilder.length());
            mFmt.format("%02d", mArgs); //$NON-NLS-1$
            return mFmt.toString();
        }
    };

    protected int incrementBy = 1;
    public void setIncrementBy(int incrementBy) {
        this.incrementBy = incrementBy;
    }

    protected final Handler  mHandler;
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

    private final LayoutInflater mInflater;
    private final TextView mText;
    protected final InputFilter mInputFilter;
    protected final InputFilter mNumberInputFilter;

    protected final Animation mSlideUpOutAnimation;
    protected final Animation mSlideUpInAnimation;
    protected final Animation mSlideDownOutAnimation;
    protected final Animation mSlideDownInAnimation;

    protected String[] mDisplayedValues;
    protected int mStart;
    protected int mEnd;
    protected int mCurrent;
    protected int mPrevious;
    private OnChangedListener mListener;
    private Formatter mFormatter;
    protected long mSpeed = 500;

    protected boolean mIncrement;
    protected boolean mDecrement;

    public NumberPickerWidget(Context context) {
        this(context, null);
    }

    public NumberPickerWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        DependencyInjectionService.getInstance().inject(this);

        setOrientation(VERTICAL);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater.inflate(numberPickerLayout, this, true);

        mHandler = new Handler(Looper.getMainLooper());
        mInputFilter = new NumberPickerInputFilter();
        mNumberInputFilter = new NumberRangeKeyListener();
        mIncrementButton = (NumberPickerWidgetButton) findViewById(numberPickerIncrementId);
        mIncrementButton.setOnClickListener(this);
        mIncrementButton.setOnLongClickListener(this);
        mIncrementButton.setNumberPicker(this);
        mDecrementButton = (NumberPickerWidgetButton) findViewById(numberPickerDecrementId);
        mDecrementButton.setOnClickListener(this);
        mDecrementButton.setOnLongClickListener(this);
        mDecrementButton.setNumberPicker(this);

        mText = (TextView) findViewById(numberPickerInputId);
        mText.setOnFocusChangeListener(this);
        mText.setFilters(new InputFilter[] { mInputFilter });

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
        if (numberPickerIncrementId == v.getId()) {
            changeCurrent(mCurrent + incrementBy, mSlideUpInAnimation,
                    mSlideUpOutAnimation);
        } else if (numberPickerDecrementId == v.getId()) {
            changeCurrent(mCurrent - incrementBy, mSlideDownInAnimation,
                    mSlideDownOutAnimation);
        }
    }

    private String formatNumber(int value) {
        return (mFormatter != null) ? mFormatter.toString(value)
                : String.valueOf(value);
    }

    protected void changeCurrent(int current,
            @SuppressWarnings("unused") Animation in,
            @SuppressWarnings("unused") Animation out) {

        // Wrap around the values if we go past the start or end
        if (current > mEnd) {
            current = mStart;
        } else if (current < mStart) {
            current = mEnd;
        }
        mPrevious = mCurrent;
        mCurrent = current;
        notifyChange();
        updateView();
    }

    private void notifyChange() {
        if (mListener != null) {
            mListener.onChanged(this, mPrevious, mCurrent);
        }
    }

    private void updateView() {

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

    private void validateCurrentView(CharSequence str) {
        int val = getSelectedPos(str.toString());
        if ((val >= mStart) && (val <= mEnd)) {
            mPrevious = mCurrent;
            mCurrent = val;
            notifyChange();
        }
        updateView();
    }

    public void onFocusChange(View v, boolean hasFocus) {

        /*
         * When focus is lost check that the text field has valid values.
         */
        if (!hasFocus && v instanceof TextView) {
            String str = String.valueOf(((TextView) v).getText());
            if ("".equals(str)) { //$NON-NLS-1$

                // Restore to the old value as we don't allow empty values
                updateView();
            } else {

                // Check the new value and ensure it's in range
                validateCurrentView(str);
            }
        }
    }

    /**
     * We start the long click here but rely on the {@link NumberPickerWidgetButton}
     * to inform us when the long click has ended.
     */
    public boolean onLongClick(View v) {

        /*
         * The text view may still have focus so clear it's focus which will
         * trigger the on focus changed and any typed values to be pulled.
         */
        mText.clearFocus();

        if (numberPickerIncrementId == v.getId()) {
            mIncrement = true;
            mHandler.post(mRunnable);
        } else if (numberPickerDecrementId == v.getId()) {
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

    protected static final char[] DIGIT_CHARACTERS = new char[] { '0', '1', '2',
        '3', '4', '5', '6', '7', '8', '9'       };

    private NumberPickerWidgetButton  mIncrementButton;
    private NumberPickerWidgetButton  mDecrementButton;

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
            return ""; //$NON-NLS-1$
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

            if ("".equals(result)) { //$NON-NLS-1$
                return result;
            }
            int val = getSelectedPos(result);

            /*
             * Ensure the user can't type in a value greater than the max
             * allowed. We have to allow less than min as the user might want to
             * delete some numbers and then type a new number.
             */
            if (val > mEnd) {
                return ""; //$NON-NLS-1$
            } else {
                return filtered;
            }
        }

		public int getInputType() {
			return 0;
		}
    }

    protected int getSelectedPos(String str) {
        if (mDisplayedValues == null) {
            return Integer.parseInt(str);
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
     * @return the current value.
     */
    public int getCurrent() {
        return mCurrent;
    }
}