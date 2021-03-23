package org.tasks.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.tasks.R

object Context {
    fun Context.safeStartActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.no_app_found)
        }
    }

    fun Context.toast(resId: Int, vararg formatArgs: Any, duration: Int = Toast.LENGTH_LONG) =
        toast(getString(resId, formatArgs), duration)

    fun Context.toast(text: String?, duration: Int = Toast.LENGTH_LONG) =
        text?.let { Toast.makeText(this, it, duration).show() }
}
