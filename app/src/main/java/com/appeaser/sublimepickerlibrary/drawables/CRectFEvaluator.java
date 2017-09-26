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

package com.appeaser.sublimepickerlibrary.drawables;

import android.animation.TypeEvaluator;
import android.graphics.RectF;

/**
 * This evaluator can be used to perform type interpolation between RectF values.
 * It is a modified version of 'RectEvaluator'
 */
public class CRectFEvaluator implements TypeEvaluator<RectF> {

    /**
     * When null, a new Rect is returned on every evaluate call. When non-null,
     * mRect will be modified and returned on every evaluate.
     */
    private RectF mRectF;

    /**
     * Construct a RectEvaluator that returns a new Rect on every evaluate call.
     * To avoid creating an object for each evaluate call,
     * {@link CRectFEvaluator#CRectFEvaluator(RectF)} should be used
     * whenever possible.
     */
    public CRectFEvaluator() {
    }

    /**
     * Constructs a RectEvaluator that modifies and returns <code>reuseRect</code>
     * in #evaluate(float, android.graphics.RectF, android.graphics.Rect) calls.
     * The value returned from
     * #evaluate(float, android.graphics.RectF, android.graphics.Rect) should
     * not be cached because it will change over time as the object is reused on each
     * call.
     *
     * @param reuseRect A Rect to be modified and returned by evaluate.
     */
    public CRectFEvaluator(RectF reuseRect) {
        mRectF = reuseRect;
    }

    /**
     * This function returns the result of linearly interpolating the start and
     * end Rect values, with <code>fraction</code> representing the proportion
     * between the start and end values. The calculation is a simple parametric
     * calculation on each of the separate components in the Rect objects
     * (left, top, right, and bottom).
     * <p>If #CRectFEvaluator(android.graphics.Rect) was used to construct
     * this RectEvaluator, the object returned will be the <code>reuseRect</code>
     * passed into the constructor.</p>
     *
     * @param fraction   The fraction from the starting to the ending values
     * @param startValue The start Rect
     * @param endValue   The end Rect
     * @return A linear interpolation between the start and end values, given the
     * <code>fraction</code> parameter.
     */
    @Override
    public RectF evaluate(float fraction, RectF startValue, RectF endValue) {
        float left = startValue.left + (endValue.left - startValue.left) * fraction;
        float top = startValue.top + (endValue.top - startValue.top) * fraction;
        float right = startValue.right + (endValue.right - startValue.right) * fraction;
        float bottom = startValue.bottom + (endValue.bottom - startValue.bottom) * fraction;
        if (mRectF == null) {
            return new RectF(left, top, right, bottom);
        } else {
            mRectF.set(left, top, right, bottom);
            return mRectF;
        }
    }
}
