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
     * @return an intent to launch market with this package
     */
    abstract public Intent generateMarketLink(String packageName);

    public static class AndroidMarketStrategy extends MarketStrategy {
        @Override
        public Intent generateMarketLink(String packageName) {
            return new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=pname:" + //$NON-NLS-1$
                            packageName));
        }
    }
}
