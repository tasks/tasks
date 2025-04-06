/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.andlib.utility

import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Looper
import android.text.InputType
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import org.tasks.BuildConfig

/**
 * Android Utility Classes
 *
 * @author Tim Su <tim></tim>@todoroo.com>
 */
object AndroidUtilities {
    // --- utility methods
    /** Suppress virtual keyboard until user's first tap  */
    @JvmStatic
    fun suppressVirtualKeyboard(editor: TextView) {
        val inputType = editor.inputType
        editor.inputType = InputType.TYPE_NULL
        editor.setOnTouchListener { v: View?, event: MotionEvent? ->
            editor.inputType = inputType
            editor.setOnTouchListener(null)
            false
        }
    }

    fun convertDpToPixels(displayMetrics: DisplayMetrics, dp: Int): Int {
        // developer.android.com/guide/practices/screens_support.html#dips-pels
        return (dp * displayMetrics.density + 0.5f).toInt()
    }

    fun preOreo(): Boolean {
        return !atLeastOreo()
    }

    fun preS(): Boolean {
        return !atLeastS()
    }

    @JvmStatic
    fun preTiramisu(): Boolean {
        return !atLeastTiramisu()
    }

    fun preUpsideDownCake(): Boolean {
        return Build.VERSION.SDK_INT <= VERSION_CODES.TIRAMISU
    }

    fun atLeastNougatMR1(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.N_MR1
    }

    @JvmStatic
    fun atLeastOreo(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.O
    }

    fun atLeastOreoMR1(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.O_MR1
    }

    fun atLeastP(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.P
    }

    @JvmStatic
    fun atLeastQ(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.Q
    }

    @JvmStatic
    fun atLeastR(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.R
    }

    @JvmStatic
    fun atLeastS(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.S
    }

    @JvmStatic
    fun atLeastTiramisu(): Boolean {
        return Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU
    }

    fun assertMainThread() {
        check(!(BuildConfig.DEBUG && !isMainThread)) { "Should be called from main thread" }
    }

    fun assertNotMainThread() {
        check(!(BuildConfig.DEBUG && isMainThread)) { "Should not be called from main thread" }
    }

    private val isMainThread: Boolean
        get() = Thread.currentThread() === Looper.getMainLooper().thread
}
