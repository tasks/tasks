/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright 2015 Vikram Kakkar
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

package com.appeaser.sublimepickerlibrary.recurrencepicker;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ToggleButton;

import com.appeaser.sublimepickerlibrary.drawables.CheckableDrawable;

public class WeekButton extends ToggleButton {

    private static int mDefaultTextColor, mCheckedTextColor;

    // Drawable that provides animations between
    // 'on' & 'off' states
    private CheckableDrawable mDrawable;

    // Flag to disable animation on state change
    private boolean noAnimate = false;

    public WeekButton(Context context) {
        super(context);
    }

    public WeekButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WeekButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    // Syncs state
    private CheckableDrawable.OnAnimationDone mCallback = new CheckableDrawable.OnAnimationDone() {
        @Override
        public void animationIsDone() {
            setTextColor(isChecked() ? mCheckedTextColor : mDefaultTextColor);
            mDrawable.setChecked(isChecked());
        }

        @Override
        public void animationHasBeenCancelled() {
            setTextColor(isChecked() ? mCheckedTextColor : mDefaultTextColor);
            mDrawable.setChecked(isChecked());
        }
    };

    // Wrapper for 'setChecked(boolean)' that does not trigger
    // state-animation
    public void setCheckedNoAnimate(boolean checked) {
        noAnimate = true;
        setChecked(checked);
        noAnimate = false;
    }

    @Override
    public void setChecked(final boolean checked) {
        super.setChecked(checked);

        if (mDrawable != null) {
            if (noAnimate) {
                mDrawable.setChecked(checked);
                setTextColor(isChecked() ? mCheckedTextColor : mDefaultTextColor);
            } else {
                // Reset text color for animation
                // The correct state color will be
                // set when animation is done or cancelled
                setTextColor(mCheckedTextColor);
                mDrawable.setCheckedOnClick(isChecked(), mCallback);
            }
        }
    }

    @Override
    public void setBackgroundDrawable(Drawable d) {
        super.setBackgroundDrawable(d);

        if (d instanceof CheckableDrawable) {
            mDrawable = (CheckableDrawable) d;
        } else {
            // Reset: in case setBackgroundDrawable
            // is called more than once
            mDrawable = null;
        }
    }

    // State-dependent text-colors
    public static void setStateColors(int defaultColor, int checkedColor) {
        mDefaultTextColor = defaultColor;
        mCheckedTextColor = checkedColor;
    }
}
