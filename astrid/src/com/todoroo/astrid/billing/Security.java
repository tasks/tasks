// Copyright 2010 Google Inc. All Rights Reserved.

package com.todoroo.astrid.billing;

import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;

import com.todoroo.astrid.billing.BillingConstants.PurchaseState;


/**
 * This is a stub class
 * @author Sam
 */
@SuppressWarnings("nls")
public class Security {
    private static final String TAG = "Security";

    private static final String KEY_FACTORY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static HashSet<Long> sKnownNonces = new HashSet<Long>();

    /**
     * A class to hold the verified purchase information.
     */
    public static class VerifiedPurchase {
        public PurchaseState purchaseState;
        public String notificationId;
        public String productId;
        public String orderId;
        public long purchaseTime;
        public String developerPayload;
        public String purchaseToken;

        public VerifiedPurchase(PurchaseState purchaseState, String notificationId,
                String productId, String orderId, long purchaseTime, String developerPayload, String purchaseToken) {
            this.purchaseState = purchaseState;
            this.notificationId = notificationId;
            this.productId = productId;
            this.orderId = orderId;
            this.purchaseTime = purchaseTime;
            this.developerPayload = developerPayload;
            this.purchaseToken = purchaseToken;
        }
    }

    /** Generates a nonce (a random number used once). */
    public static long generateNonce() {
        long nonce = RANDOM.nextLong();
        sKnownNonces.add(nonce);
        return nonce;
    }

    public static void removeNonce(long nonce) {
        sKnownNonces.remove(nonce);
    }

    public static boolean isNonceKnown(long nonce) {
        return sKnownNonces.contains(nonce);
    }

    public static ArrayList<VerifiedPurchase> verifyPurchase(String signedData, String signature) {
        return null;
    }

    private static String constructPublicKey() {
        return "";
    }

    public static PublicKey generatePublicKey(String encodedPublicKey) {
        return null;
    }

    public static boolean verify(PublicKey publicKey, String signedData, String signature) {
        return false;
    }
}
