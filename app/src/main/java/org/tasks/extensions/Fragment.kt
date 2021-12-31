package org.tasks.extensions

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.fragment.app.Fragment
import org.tasks.R
import org.tasks.extensions.Context.openUri

object Fragment {
    fun Fragment.safeStartActivityForResult(intent: Intent, rc: Int) {
        try {
            startActivityForResult(intent, rc)
        } catch (e: ActivityNotFoundException) {
            context?.openUri(R.string.url_troubleshoot_intents)
        }
    }
}