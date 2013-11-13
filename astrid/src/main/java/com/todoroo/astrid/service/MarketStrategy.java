/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.content.Intent;
import android.net.Uri;

import org.tasks.R;

public abstract class MarketStrategy {

    /**
     * @return an intent to launch market with this package
     */
    abstract public Intent generateMarketLink(String packageName);

    public int[] excludedSettings() {
        return null;
    }

    public static class AndroidMarketStrategy extends MarketStrategy {

        @Override
        public Intent generateMarketLink(String packageName) {
            return new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=pname:" + //$NON-NLS-1$
                            packageName));
        }
    }

    public static class AmazonMarketStrategy extends MarketStrategy {

        @Override
        public Intent generateMarketLink(String packageName) {
            return new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=" + //$NON-NLS-1$
                            packageName));
        }

        /**
         * @return true if the device is a kindle fire and needs special treatment
         */
        public static boolean isKindleFire() {
            return android.os.Build.MANUFACTURER.equals("Amazon") && //$NON-NLS-1$
                android.os.Build.MODEL.contains("Kindle"); //$NON-NLS-1$
        }

        @Override
        public int[] excludedSettings() {
            return new int[] {
                R.string.p_voicePrefSection,
                R.string.p_end_at_deadline,
                R.string.p_field_missed_calls
            };
        }
    }
}
