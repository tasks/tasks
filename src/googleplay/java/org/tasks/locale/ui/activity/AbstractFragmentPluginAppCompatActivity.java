package org.tasks.locale.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.injection.ThemedInjectingAppCompatActivity;

import timber.log.Timber;

public abstract class AbstractFragmentPluginAppCompatActivity extends ThemedInjectingAppCompatActivity {

    protected boolean mIsCancelled = false;

    /* package */ static boolean isLocalePluginIntent(final Intent intent) {
        final String action = intent.getAction();

        return com.twofortyfouram.locale.api.Intent.ACTION_EDIT_CONDITION.equals(action)
                || com.twofortyfouram.locale.api.Intent.ACTION_EDIT_SETTING.equals(action);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isLocalePluginIntent(getIntent())) {
            final Bundle previousBundle = getPreviousBundle();

            Timber.d("Creating Activity with Intent=%s, savedInstanceState=%s, EXTRA_BUNDLE=%s",
                    getIntent(), savedInstanceState, previousBundle); //$NON-NLS-1$
        }
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (isLocalePluginIntent(getIntent())) {
            if (null == savedInstanceState) {
                final Bundle previousBundle = getPreviousBundle();
                final String previousBlurb = getPreviousBlurb();
                if (null != previousBundle && null != previousBlurb) {
                    onPostCreateWithPreviousResult(previousBundle, previousBlurb);
                }
            }
        }
    }

    @Override
    public void finish() {
        if (isLocalePluginIntent(getIntent())) {
            if (!mIsCancelled) {
                final Bundle resultBundle = getResultBundle();

                if (null != resultBundle) {
                    String blurb = getResultBlurb(resultBundle);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE,
                            resultBundle);
                    resultIntent.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB,
                            blurb);
                    setResult(RESULT_OK, resultIntent);
                }
            }
        }
        super.finish();
    }

    /**
     * @return The {@link com.twofortyfouram.locale.api.Intent#EXTRA_BUNDLE EXTRA_BUNDLE} that was
     * previously saved to the host and subsequently passed back to this Activity for further
     * editing.  Internally, this method relies on {@link #isBundleValid(Bundle)}.  If
     * the bundle exists but is not valid, this method will return null.
     */
    public final Bundle getPreviousBundle() {
        final Bundle bundle = getIntent().getBundleExtra(
                com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);

        if (null != bundle) {
            if (isBundleValid(bundle)) {
                return bundle;
            }
        }

        return null;
    }

    /**
     * @return The {@link com.twofortyfouram.locale.api.Intent#EXTRA_STRING_BLURB
     * EXTRA_STRING_BLURB} that was
     * previously saved to the host and subsequently passed back to this Activity for further
     * editing.
     */
    public final String getPreviousBlurb() {
        return getIntent().getStringExtra(
                com.twofortyfouram.locale.api.Intent.EXTRA_STRING_BLURB);
    }

    /**
     * <p>Validates the Bundle, to ensure that a malicious application isn't attempting to pass
     * an invalid Bundle.</p>
     *
     * @param bundle The plug-in's Bundle previously returned by the edit
     *               Activity.  {@code bundle} should not be mutated by this method.
     * @return true if {@code bundle} is valid for the plug-in.
     */
    public abstract boolean isBundleValid(final Bundle bundle);

    /**
     * Plug-in Activity lifecycle callback to allow the Activity to restore
     * state for editing a previously saved plug-in instance. This callback will
     * occur during the onPostCreate() phase of the Activity lifecycle.
     * <p>{@code bundle} will have been
     * validated by {@link #isBundleValid(Bundle)} prior to this
     * method being called.  If {@link #isBundleValid(Bundle)} returned false, then this
     * method will not be called.  This helps ensure that plug-in Activity subclasses only have to
     * worry about bundle validation once, in the {@link #isBundleValid(Bundle)}
     * method.</p>
     * <p>Note this callback only occurs the first time the Activity is created, so it will not be
     * called
     * when the Activity is recreated (e.g. {@code savedInstanceState != null}) such as after a
     * configuration change like a screen rotation.</p>
     *
     * @param previousBundle Previous bundle that the Activity saved.
     * @param previousBlurb  Previous blurb that the Activity saved
     */
    public abstract void onPostCreateWithPreviousResult(
            final Bundle previousBundle, final String previousBlurb);

    /**
     * @return Bundle for the plug-in or {@code null} if a valid Bundle cannot
     * be generated.
     */
    public abstract Bundle getResultBundle();

    /**
     * @param bundle Valid bundle for the component.
     * @return Blurb for {@code bundle}.
     */
    public abstract String getResultBlurb(final Bundle bundle);
}