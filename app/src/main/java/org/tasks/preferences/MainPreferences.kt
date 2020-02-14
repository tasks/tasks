package org.tasks.preferences

import android.content.Intent
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import org.tasks.R
import org.tasks.injection.ActivityComponent
import org.tasks.preferences.fragments.MainSettingsFragment

class MainPreferences : BasePreferences(), Toolbar.OnMenuItemClickListener {

    override fun setupMenu() {
        toolbar.inflateMenu(R.menu.menu_preferences)
        toolbar.setOnMenuItemClickListener(this)
    }

    override fun getRootTitle() = R.string.TLA_menu_settings

    override fun getRootPreference() = MainSettingsFragment()

    override fun inject(component: ActivityComponent) {
        component.inject(this)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return if (item?.itemId == R.id.menu_help_and_feedback) {
            startActivity(Intent(this, HelpAndFeedback::class.java))
            true
        } else {
            false
        }
    }
}