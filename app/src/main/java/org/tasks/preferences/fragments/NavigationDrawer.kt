package org.tasks.preferences.fragments

import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.preferences.IconPreference

@AndroidEntryPoint
class NavigationDrawer : InjectingPreferenceFragment() {

    override fun getPreferenceXml() = R.xml.preferences_navigation_drawer

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        (findPreference(R.string.customize_drawer) as IconPreference).apply {
            drawable = AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.ic_keyboard_arrow_right_24px
            )?.mutate()
            tint = context.getColor(R.color.icon_tint_with_alpha)
            iconVisible = true
        }
    }
}