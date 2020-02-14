package org.tasks.injection

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.jakewharton.processphoenix.ProcessPhoenix
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.api.Filter
import org.tasks.R
import org.tasks.dialogs.DialogBuilder
import org.tasks.preferences.Device
import javax.inject.Inject

abstract class InjectingPreferenceFragment : PreferenceFragmentCompat() {

    @Inject lateinit var device: Device
    @Inject lateinit var dialogBuilder: DialogBuilder

    private var injected = false

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (!injected) {
            inject((activity as InjectingActivity).component.plus(FragmentModule(this)))
            injected = true
        }
    }

    protected open fun showRestartDialog() {
        dialogBuilder
                .newDialog()
                .setMessage(R.string.restart_required)
                .setPositiveButton(R.string.restart_now) { _: DialogInterface?, _: Int ->
                    val nextIntent = Intent(context, MainActivity::class.java)
                    nextIntent.putExtra(MainActivity.OPEN_FILTER, null as Filter?)
                    ProcessPhoenix.triggerRebirth(context, nextIntent)
                }
                .setNegativeButton(R.string.restart_later, null)
                .show()
    }

    protected fun requires(@StringRes prefGroup: Int, check: Boolean, vararg resIds: Int) {
        if (!check) {
            remove(findPreference(prefGroup) as PreferenceGroup, resIds)
        }
    }

    protected fun remove(vararg resIds: Int) {
        remove(preferenceScreen, resIds)
    }

    private fun remove(preferenceGroup: PreferenceGroup, resIds: IntArray) {
        for (resId in resIds) {
            preferenceGroup.removePreference(findPreference(resId))
        }
    }

    protected fun recreate() {
        activity!!.recreate()
    }

    protected fun findPreference(@StringRes prefId: Int): Preference {
        return findPreference(getString(prefId))!!
    }

    protected abstract fun inject(component: FragmentComponent)
}