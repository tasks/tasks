/* Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.vending.billing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receiver for the "com.android.vending.billing.PURCHASES_UPDATED" Action
 * from the Play Store.
 *
 * <p>It is possible that an in-app item may be acquired without the
 * application calling getBuyIntent(), for example if the item can be
 * redeemed from inside the Play Store using a promotional code. If this
 * application isn't running at the time, then when it is started a call
 * to getPurchases() will be sufficient notification. However, if the
 * application is already running in the background when the item is acquired,
 * a message to this BroadcastReceiver will indicate that the an item
 * has been acquired.</p>
 */
public class IabBroadcastReceiver extends BroadcastReceiver {
    /**
     * Listener interface for received broadcast messages.
     */
    public interface IabBroadcastListener {
        void receivedBroadcast();
    }

    /**
     * The Intent action that this Receiver should filter for.
     */
    public static final String ACTION = "com.android.vending.billing.PURCHASES_UPDATED";

    private final IabBroadcastListener mListener;

    public IabBroadcastReceiver(IabBroadcastListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mListener != null) {
            mListener.receivedBroadcast();
        }
    }
}
