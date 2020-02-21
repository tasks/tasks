package org.tasks.locale.ui.activity

import android.content.Intent
import android.os.Bundle
import org.tasks.preferences.BasePreferences
import timber.log.Timber

abstract class AbstractFragmentPluginPreference : BasePreferences() {

    companion object {
        fun isLocalePluginIntent(intent: Intent): Boolean {
            val action = intent.action
            return com.twofortyfouram.locale.api.Intent.ACTION_EDIT_CONDITION == action
                    || com.twofortyfouram.locale.api.Intent.ACTION_EDIT_SETTING == action
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isLocalePluginIntent(intent)) {
            Timber.d(
                "Creating Activity with Intent=%s, savedInstanceState=%s, EXTRA_BUNDLE=%s",
                intent, savedInstanceState, getPreviousBundle()
            )
        }
    }

    override fun finish() {
        if (isLocalePluginIntent(intent)) {
            if (!isCancelled()) {
                val resultBundle = getResultBundle()
                if (null != resultBundle) {
                    val blurb = getResultBlurb(resultBundle)
                    val result = Bundle()
                    result.putBundle(
                        com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE,
                        resultBundle
                    )
                    result.putString(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB, blurb)
                    setResult(RESULT_OK, Intent().putExtras(result))
                }
            }
        }
        super.finish()
    }

    /**
     * @return The [EXTRA_BUNDLE][com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE] that was
     * previously saved to the host and subsequently passed back to this Activity for further
     * editing. Internally, this method relies on [.isBundleValid]. If the bundle
     * exists but is not valid, this method will return null.
     */
    protected fun getPreviousBundle(): Bundle? {
        val bundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE)
        if (null != bundle) {
            if (isBundleValid(bundle)) {
                return bundle
            }
        }
        return null
    }

    /**
     * Validates the Bundle, to ensure that a malicious application isn't attempting to pass an
     * invalid Bundle.
     *
     * @param bundle The plug-in's Bundle previously returned by the edit Activity. `bundle`
     * should not be mutated by this method.
     * @return true if `bundle` is valid for the plug-in.
     */
    protected abstract fun isBundleValid(bundle: Bundle?): Boolean

    /** @return Bundle for the plug-in or `null` if a valid Bundle cannot be generated.
     */
    protected abstract fun getResultBundle(): Bundle?

    /**
     * @param bundle Valid bundle for the component.
     * @return Blurb for `bundle`.
     */
    protected abstract fun getResultBlurb(bundle: Bundle?): String?

    protected abstract fun isCancelled(): Boolean
}