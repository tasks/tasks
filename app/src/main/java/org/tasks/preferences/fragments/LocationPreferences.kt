package org.tasks.preferences.fragments

import android.os.Bundle
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Tasks.Companion.IS_GOOGLE_PLAY
import org.tasks.billing.Inventory
import org.tasks.gtasks.PlayServices
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.ui.Toaster
import javax.inject.Inject

@AndroidEntryPoint
class LocationPreferences : InjectingPreferenceFragment() {

    @Inject lateinit var playServices: PlayServices
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var toaster: Toaster

    override fun getPreferenceXml() = R.xml.preferences_location

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        if (IS_GOOGLE_PLAY) {
            findPreference(R.string.p_place_provider)
                    .setOnPreferenceChangeListener(this::onPlaceSearchChanged)
        } else {
            disable(
                    R.string.p_place_provider,
            )
        }
    }

    private fun onPlaceSearchChanged(preference: Preference, newValue: Any): Boolean =
            if (newValue.toString().toIntOrNull() ?: 0 == 1) {
                if (!playServices.refreshAndCheck()) {
                    playServices.resolve(activity)
                    false
                } else if (!inventory.hasPro) {
                    toaster.longToast(R.string.requires_pro_subscription)
                    false
                } else {
                    true
                }
            } else {
                true
            }
}