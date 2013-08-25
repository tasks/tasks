/*
 * Copyright (C) 2010 Cyril Mottier (http://www.cyrilmottier.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package greendroid.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.cyrilmottier.android.greendroid.R;

/**
 * A SegmentedBar displays a set of Buttons. Only one segment in a SegmentedBar
 * can be selected at a time. A SegmentedBar is very close to a TabWidget in
 * term of functionalities.
 * 
 * @author Cyril Mottier
 */
public class SegmentedBar extends LinearLayout implements OnFocusChangeListener {

    /**
     * Clients may use this listener to be notified of any changes that occurs
     * on the SegmentBar
     * 
     * @author Cyril Mottier
     */
    public static interface OnSegmentChangeListener {
        /**
         * Notification that the current segment has changed.
         * 
         * @param index The index of the new selected segment.
         * @param clicked Whether the segment has been selected via a user
         *            click.
         */
        public void onSegmentChange(int index, boolean clicked);
    }

    private OnSegmentChangeListener mOnSegmentChangeListener;
    private int mCheckedSegment;
    private Drawable mDividerDrawable;
    private int mDividerWidth;

    public SegmentedBar(Context context) {
        this(context, null);
    }

    public SegmentedBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.gdSegmentedBarStyle);
    }

    public SegmentedBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        initSegmentedBar();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SegmentedBar, defStyle, 0);

        mDividerDrawable = a.getDrawable(R.styleable.SegmentedBar_dividerDrawable);
        mDividerWidth = a.getDimensionPixelSize(R.styleable.SegmentedBar_dividerWidth, 0);

        a.recycle();
    }

    private void initSegmentedBar() {
        mCheckedSegment = 0;
        setOrientation(LinearLayout.HORIZONTAL);

        // Register ourselves so that we can handle focus on internal segments
        setFocusable(true);
        setOnFocusChangeListener(this);
    }

    /**
     * Sets the drawable that is used as divider between each segment.
     * 
     * @param dividerDrawable The drawable to used as a divider. Note : using a
     *            ColorDrawable will not work properly as the intrinsic width of
     *            a ColorDrawable is -1.
     */
    public void setDividerDrawable(Drawable dividerDrawable) {
        mDividerDrawable = dividerDrawable;
    }

    /**
     * Sets the drawable that is used as divider between each segment.
     * 
     * @param resId The identifier of the Drawable to use.
     */
    public void setDividerDrawable(int resId) {
        mDividerDrawable = getContext().getResources().getDrawable(resId);
    }

    /**
     * Sets the width of the divider that will be used as segment divider. If
     * the dividerWidth has not been set, the intrinsic width of the divider
     * drawable is used.
     * 
     * @param width Width of the divider
     */
    public void setDividerWidth(int width) {
        mDividerWidth = width;
    }

    /**
     * Returns the current number of segment in this SegmentBar
     * 
     * @return The number of segments in this SegmentBar
     */
    public int getSegmentCount() {
        int segmentCount = getChildCount();
        // If we have divider we'll have an odd number of child
        if (mDividerDrawable != null) {
            segmentCount = (segmentCount + 1) / 2;
        }
        return segmentCount;
    }

    /**
     * Use this method to register an OnSegmentChangeListener and listen to
     * changes that occur on this SegmentBar
     * 
     * @param listener The listener to use
     */
    public void setOnSegmentChangeListener(OnSegmentChangeListener listener) {
        mOnSegmentChangeListener = listener;
    }

    /**
     * Sets the current segment to the index <em>index</em>
     * 
     * @param index The index of the segment to set. Client will be notified
     *            using this method of the segment change.
     */
    public void setCurrentSegment(int index) {
        if (index < 0 || index >= getSegmentCount()) {
            return;
        }

        ((CheckBox) getChildSegmentAt(mCheckedSegment)).setChecked(false);
        mCheckedSegment = index;
        ((CheckBox) getChildSegmentAt(mCheckedSegment)).setChecked(true);
    }

    /**
     * Returns the view representing the segment at the index <em>index</em>
     * 
     * @param index The index of the segment to retrieve
     * @return The view that represents the segment at index <em>index</em>
     */
    public View getChildSegmentAt(int index) {
        /*
         * If we are using dividers, then instead of segments at 0, 1, 2, ... we
         * have segments at 0, 2, 4, ...
         */
        if (mDividerDrawable != null) {
            index *= 2;
        }
        return getChildAt(index);
    }

    /**
     * Adds a segment to the SegmentBar. This method automatically adds a
     * divider if needed.
     * 
     * @param title The title of the segment to add.
     */
    public void addSegment(String title) {

        final Context context = getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);

        /*
         * First of all, we have to check whether or not we need to add a
         * divider. A divider is added when there is at least one segment
         */
        if (mDividerDrawable != null && getSegmentCount() > 0) {
            ImageView divider = new ImageView(context);
            final int width = (mDividerWidth > 0) ? mDividerWidth : mDividerDrawable.getIntrinsicWidth();
            final LinearLayout.LayoutParams lp = new LayoutParams(width, LayoutParams.FILL_PARENT);
            lp.setMargins(0, 0, 0, 0);
            divider.setLayoutParams(lp);
            divider.setBackgroundDrawable(mDividerDrawable);
            addView(divider);
        }

        CheckBox segment = (CheckBox) inflater.inflate(R.layout.gd_segment, this, false);
        segment.setText(title);

        segment.setClickable(true);
        segment.setFocusable(true);
        segment.setOnFocusChangeListener(this);
        segment.setOnCheckedChangeListener(new SegmentCheckedListener(getSegmentCount()));
        segment.setOnClickListener(new SegmentClickedListener(getSegmentCount()));

        addView(segment);
    }

    public void onFocusChange(View v, boolean hasFocus) {

        if (!hasFocus) {
            return;
        }

        if (v == this) {
            final View segment = getChildSegmentAt(mCheckedSegment);
            if (segment != null) {
                segment.requestFocus();
            }
        }

        else {
            final int segmentCounts = getSegmentCount();
            for (int i = 0; i < segmentCounts; i++) {
                if (getChildSegmentAt(i) == v) {
                    setCurrentSegment(i);
                    notifyListener(i, false);
                    break;
                }
            }
        }
    }

    private class SegmentClickedListener implements OnClickListener {

        private final int mSegmentIndex;

        private SegmentClickedListener(int segmentIndex) {
            mSegmentIndex = segmentIndex;
        }

        public void onClick(View v) {
            final CheckBox segment = (CheckBox) getChildSegmentAt(mCheckedSegment);
            if (mSegmentIndex == mCheckedSegment && !segment.isChecked()) {
                segment.setChecked(true);
            } else {
                notifyListener(mSegmentIndex, true);
            }
        }

    }

    private class SegmentCheckedListener implements OnCheckedChangeListener {

        private final int mSegmentIndex;

        private SegmentCheckedListener(int segmentIndex) {
            mSegmentIndex = segmentIndex;
        }

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                setCurrentSegment(mSegmentIndex);
            }
        }
    }

    private void notifyListener(int index, boolean clicked) {
        if (mOnSegmentChangeListener != null) {
            mOnSegmentChangeListener.onSegmentChange(index, clicked);
        }
    }
}
