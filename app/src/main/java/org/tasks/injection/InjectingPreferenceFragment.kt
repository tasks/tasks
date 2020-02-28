package org.tasks.injection

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import com.jakewharton.processphoenix.ProcessPhoenix
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.api.Filter
import org.tasks.R
import org.tasks.dialogs.DialogBuilder
import org.tasks.preferences.Device
import javax.inject.Inject


abstract class InjectingPreferenceFragment : PreferenceFragmentCompat() {

    companion object {
        fun tintIcons(pref: Preference, color: Int) {
            if (pref is PreferenceGroup) {
                for (i in 0 until pref.preferenceCount) {
                    tintIcons(pref.getPreference(i), color)
                }
            } else {
                val icon: Drawable? = pref.icon
                if (icon != null) {
                    DrawableCompat.setTint(icon, color)
                }
            }
        }
    }

    @Inject lateinit var device: Device
    @Inject lateinit var dialogBuilder: DialogBuilder

    private var injected = false

    final override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(getPreferenceXml(), rootKey)

        tintIcons(preferenceScreen, ContextCompat.getColor(context!!, R.color.icon_tint))

        setupPreferences(savedInstanceState)
    }

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

    protected fun tintIcon(resId: Int, tint: Int) {
        val pref = findPreference(resId)
        val icon = DrawableCompat.wrap(pref.icon.mutate())
        DrawableCompat.setTint(if (icon is LayerDrawable) icon.getDrawable(0) else icon, tint)
        pref.icon = icon
    }

    protected fun requires(@StringRes prefGroup: Int, check: Boolean, vararg resIds: Int) {
        if (!check) {
            remove(findPreference(prefGroup) as PreferenceGroup, resIds)
        }
    }

    protected fun requires(check: Boolean, vararg resIds: Int) {
        if (!check) {
            remove(preferenceScreen as PreferenceGroup, resIds)
        }
    }

    protected fun removeGroup(key: Int) {
        val preference = findPreference(key)
        (findPreference(R.string.preference_screen) as PreferenceScreen).removePreference(preference)
    }

    protected fun remove(vararg resIds: Int) {
        remove(preferenceScreen, resIds)
    }

    private fun remove(preferenceGroup: PreferenceGroup, resIds: IntArray) {
        for (resId in resIds) {
            val preference: Preference? = preferenceGroup.findPreference(getString(resId))
            if (preference != null) {
                preferenceGroup.removePreference(preference)
            }
        }
    }

    open fun getMenu() = R.menu.menu_preferences

    abstract fun getPreferenceXml(): Int

    abstract fun setupPreferences(savedInstanceState: Bundle?)

    protected fun recreate() {
        activity!!.recreate()
    }

    protected fun findPreference(@StringRes prefId: Int): Preference {
        return findPreference(getString(prefId))!!
    }

    protected abstract fun inject(component: FragmentComponent)
}