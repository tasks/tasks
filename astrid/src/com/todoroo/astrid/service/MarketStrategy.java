/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import android.content.Intent;
import android.net.Uri;

public abstract class MarketStrategy {

    /**
     * @param packageName
     * @return an intent to launch market with this package
     */
    abstract public Intent generateMarketLink(String packageName);

    abstract public String strategyId();

    /**
     * @return if this market has power pack
     */
    public boolean includesPowerPack() {
        return true;
    }

    /**
     * @return if this market has locale plugin
     */
    public boolean includesLocalePlugin() {
        return true;
    }

    public int[] excludedSettings() {
        return null;
    }

    /**
     * @return true if ideas tab should be shown
     */
    public boolean allowIdeasTab() {
        return true;
    }

    /**
     * Most market strategies don't support billing at this time,
     * so we'll make the default false
     * @return
     */
    public boolean billingSupported() {
        return false;
    }

    /**
     * Return true if the preference to use the phone layout should be
     * turned on by default (only true for Nook)
     * @return
     */
    public boolean defaultPhoneLayout() {
        return false;
    }

    public static class NoMarketStrategy extends MarketStrategy {
        @Override
        public Intent generateMarketLink(String packageName) {
            return null;
        }

        @Override
        public String strategyId() {
            return "no_market"; //$NON-NLS-1$
        }
    }

    public static class AndroidMarketStrategy extends MarketStrategy {

        @Override
        public Intent generateMarketLink(String packageName) {
            return new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=pname:" + //$NON-NLS-1$
                            packageName));
        }

        @Override
        public String strategyId() {
            return "android_market"; //$NON-NLS-1$
        }

        @Override
        public boolean billingSupported() {
            return true;
        }

    }

}
