/*
 * Copyright 2015 Vikram Kakkar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appeaser.sublimepickerlibrary.drawables;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Provides animated transition between 'on' and 'off' state.
 * Used as background for 'WeekButton'.
 */
public class CheckableDrawable extends Drawable {

    private final int ANIMATION_DURATION_EXPAND = 500, ANIMATION_DURATION_COLLAPSE = 400;
    private int mMinAlpha, mMaxAlpha;
    private Paint mPaint;

    private AnimatorSet asTransition;
    private final OvershootInterpolator mExpandInterpolator = new OvershootInterpolator();
    private final AnticipateInterpolator mCollapseInterpolator = new AnticipateInterpolator();
    private final CRectFEvaluator mRectEvaluator = new CRectFEvaluator();

    private RectF mRectToDraw, mCollapsedRect, mExpandedRect;
    private int mExpandedWidthHeight;

    private boolean mChecked, mReady;

    public CheckableDrawable(int color, boolean checked, int expandedWidthHeight) {
        mChecked = checked;
        mExpandedWidthHeight = expandedWidthHeight;

        mMaxAlpha = Color.alpha(color);
        // Todo: Provide an option to change this value
        mMinAlpha = 0;

        mRectToDraw = new RectF();
        mExpandedRect = new RectF();
        mCollapsedRect = new RectF();
        mPaint = new Paint();
        mPaint.setColor(color);
        mPaint.setAlpha(mMaxAlpha);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
    }

    // initialize dimensions
    private void setDimens(int width, int height) {
        mReady = true;

        float expandedLeft = (width - mExpandedWidthHeight) / 2f;
        float expandedTop = (height - mExpandedWidthHeight) / 2f;
        float expandedRight = (width + mExpandedWidthHeight) / 2f;
        float expandedBottom = (height + mExpandedWidthHeight) / 2f;

        float collapsedLeft = width / 2f;
        float collapsedTop = height / 2f;
        float collapsedRight = width / 2f;
        float collapsedBottom = height / 2f;

        mCollapsedRect = new RectF(collapsedLeft, collapsedTop,
                collapsedRight, collapsedBottom);
        mExpandedRect = new RectF(expandedLeft, expandedTop,
                expandedRight, expandedBottom);

        reset();
    }

    // Called when 'WeekButton' checked state changes
    public void setCheckedOnClick(boolean checked, final OnAnimationDone callback) {
        mChecked = checked;
        if (!mReady) {
            invalidateSelf();
            return;
        }
        reset();
        onClick(callback);
    }

    private void onClick(final OnAnimationDone callback) {
        animate(mChecked, callback);
    }

    private void cancelAnimationInTracks() {
        if (asTransition != null && asTransition.isRunning()) {
            asTransition.cancel();
        }
    }

    // Set state without animation
    public void setChecked(boolean checked) {
        if (mChecked == checked)
            return;

        mChecked = checked;
        reset();
    }

    private void reset() {
        cancelAnimationInTracks();

        if (mChecked) {
            mRectToDraw.set(mExpandedRect);
        } else {
            mRectToDraw.set(mCollapsedRect);
        }

        invalidateSelf();
    }

    // Animate between 'on' & 'off' state
    private void animate(boolean expand, final OnAnimationDone callback) {
        RectF from = expand ? mCollapsedRect : mExpandedRect;
        RectF to = expand ? mExpandedRect : mCollapsedRect;

        mRectToDraw.set(from);

        ObjectAnimator oaTransition = ObjectAnimator.ofObject(this,
                "newRectBounds",
                mRectEvaluator, from, to);

        int duration = expand ?
                ANIMATION_DURATION_EXPAND :
                ANIMATION_DURATION_COLLAPSE;

        oaTransition.setDuration(duration);
        oaTransition.setInterpolator(expand ?
                mExpandInterpolator :
                mCollapseInterpolator);

        ObjectAnimator oaAlpha = ObjectAnimator.ofInt(this,
                "alpha",
                expand ? mMinAlpha : mMaxAlpha,
                expand ? mMaxAlpha : mMinAlpha);
        oaAlpha.setDuration(duration);

        asTransition = new AnimatorSet();
        asTransition.playTogether(oaTransition, oaAlpha);

        asTransition.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                if (callback != null) {
                    callback.animationIsDone();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);

                if (callback != null) {
                    callback.animationHasBeenCancelled();
                }
            }
        });

        asTransition.start();
    }

    @Override
    public void draw(Canvas canvas) {
        if (!mReady) {
            setDimens(getBounds().width(), getBounds().height());
            return;
        }

        canvas.drawOval(mRectToDraw, mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    // ObjectAnimator property
    @SuppressWarnings("unused")
    public void setNewRectBounds(RectF newRectBounds) {
        mRectToDraw = newRectBounds;
        invalidateSelf();
    }

    // Callback
    public interface OnAnimationDone {
        void animationIsDone();

        void animationHasBeenCancelled();
    }
}
