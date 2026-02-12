package org.tasks.preferences

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.tasks.R
import org.tasks.analytics.Firebase
import org.tasks.databinding.ActivityPreferencesBinding
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.injection.ThemedInjectingAppCompatActivity
import javax.inject.Inject

private const val EXTRA_TITLE = "extra_title"

abstract class BasePreferences : ThemedInjectingAppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, Toolbar.OnMenuItemClickListener {

    @Inject lateinit var firebase: Firebase

    lateinit var toolbar: Toolbar
    var menu: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            ),
            navigationBarStyle = if (tasksTheme.themeBase.isDarkTheme(this)) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            }
        )

        val binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getSystemWindowInsets()
            toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top
            }
            insets
        }

        toolbar = binding.toolbar.toolbar
        if (savedInstanceState == null) {
            val rootPreference = getRootPreference()
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, rootPreference)
                .commit()
            toolbar.title = getString(getRootTitle())
            setupMenu(rootPreference)
        } else {
            toolbar.title = savedInstanceState.getCharSequence(EXTRA_TITLE)
            setupMenu()
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                toolbar.title = getString(getRootTitle())
            }
        }
        toolbar.navigationIcon =
            getDrawable(R.drawable.ic_outline_arrow_back_24px)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        toolbar.setOnMenuItemClickListener(this)
    }

    private fun setupMenu() = setupMenu(supportFragmentManager.findFragmentById(R.id.settings))

    private fun setupMenu(fragment: Fragment?) {
        menu = if (fragment is InjectingPreferenceFragment) fragment.getMenu() else 0
        toolbar.menu.clear()
        if (menu > 0) {
            toolbar.inflateMenu(menu)
        }
    }

    abstract fun getRootTitle(): Int

    abstract fun getRootPreference(): Fragment

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(EXTRA_TITLE, toolbar.title)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.popBackStackImmediate()) {
            setupMenu()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean = startPreference(
            caller,
            supportFragmentManager
                    .fragmentFactory
                    .instantiate(classLoader, pref.fragment!!)
                    .apply { arguments = pref.extras },
            pref.title!!
    )

    fun startPreference(
            caller: PreferenceFragmentCompat,
            fragment: Fragment,
            title: CharSequence
    ): Boolean {
        firebase.logEvent(
            R.string.event_settings_navigation,
            R.string.param_screen to title.toString()
        )
        @Suppress("DEPRECATION")
        fragment.setTargetFragment(caller, 0)
        supportFragmentManager.beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit()
        toolbar.title = title
        setupMenu(fragment)
        return true
    }

    fun startPreference(
            fragment: Fragment,
            title: CharSequence
    ): Boolean {
        firebase.logEvent(
            R.string.event_settings_navigation,
            R.string.param_screen to title.toString()
        )
        supportFragmentManager.beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit()
        toolbar.title = title
        setupMenu(fragment)
        return true
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean =
            if (item?.itemId == R.id.menu_help_and_feedback) {
                startActivity(
                    Intent(this, HelpAndFeedback::class.java)
                        .putExtra(HelpAndFeedback.EXTRA_SOURCE, "settings_overflow")
                )
                true
            } else false
}