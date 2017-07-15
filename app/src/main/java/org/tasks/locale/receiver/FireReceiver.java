package org.tasks.locale.receiver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.todoroo.astrid.api.Filter;

import org.tasks.Notifier;
import org.tasks.injection.BroadcastComponent;
import org.tasks.injection.InjectingBroadcastReceiver;
import org.tasks.locale.bundle.PluginBundleValues;
import org.tasks.preferences.DefaultFilterProvider;

import javax.inject.Inject;

import timber.log.Timber;

public final class FireReceiver extends InjectingBroadcastReceiver {

    @Inject Notifier notifier;
    @Inject DefaultFilterProvider defaultFilterProvider;

    @Override
    public final void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);

        Timber.d("Received %s", intent); //$NON-NLS-1$

        /*
         * Note: It is OK if a host sends an ordered broadcast for plug-in
         * settings. Such a behavior would allow the host to optionally block until the
         * plug-in setting finishes.
         */

        if (!com.twofortyfouram.locale.api.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            Timber.e("Intent action is not %s", com.twofortyfouram.locale.api.Intent.ACTION_FIRE_SETTING); //$NON-NLS-1$
            return;
        }

        /*
         * Ignore implicit intents, because they are not valid. It would be
         * meaningless if ALL plug-in setting BroadcastReceivers installed were
         * asked to handle queries not intended for them. Ideally this
         * implementation here would also explicitly assert the class name as
         * well, but then the unit tests would have trouble. In the end,
         * asserting the package is probably good enough.
         */
        if (!context.getPackageName().equals(intent.getPackage())
                && !new ComponentName(context, this.getClass().getName()).equals(intent
                .getComponent())) {
            Timber.e("Intent is not explicit"); //$NON-NLS-1$
            return;
        }

        final Bundle bundle = intent
                .getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);

        if (null == bundle) {
            Timber.e("%s is missing",
                    com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE); //$NON-NLS-1$
            return;
        }

        if (!PluginBundleValues.isBundleValid(bundle)) {
            Timber.e("%s is invalid",
                    com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE); //$NON-NLS-1$
            return;
        }

        Filter filter = defaultFilterProvider.getFilterFromPreference(
                bundle.getString(PluginBundleValues.BUNDLE_EXTRA_STRING_FILTER));
        notifier.triggerFilterNotification(filter);
    }

    @Override
    protected void inject(BroadcastComponent component) {
        component.inject(this);
    }
}
