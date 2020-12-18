package org.tasks.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.tasks.R

fun Context.safeStartActivity(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        toast(this)
    }
}

fun Fragment.safeStartActivityForResult(intent: Intent, rc: Int) {
    try {
        startActivityForResult(intent, rc)
    } catch (e: ActivityNotFoundException) {
        toast(context)
    }
}

private fun toast(context: Context?) {
    context?.let {
        Toast.makeText(it, R.string.no_app_found, Toast.LENGTH_LONG).show()
    }
}