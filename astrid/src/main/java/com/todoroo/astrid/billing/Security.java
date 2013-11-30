package com.todoroo.astrid.billing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.todoroo.astrid.billing.BillingConstants.PurchaseState;

import java.security.SecureRandom;
import java.util.ArrayList;

public class Security {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static class Purchase {
        public PurchaseState purchaseState;
        public String notificationId;
        public String productId;

        public Purchase(PurchaseState purchaseState, String notificationId, String productId) {
            this.purchaseState = purchaseState;
            this.notificationId = notificationId;
            this.productId = productId;
        }
    }

    public static long generateNonce() {
        return RANDOM.nextLong();
    }

    public static ArrayList<Purchase> parse(String signedData) {
        ArrayList<Purchase> purchases = new ArrayList<>();
        JsonElement jsonElement = new JsonParser().parse(signedData);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonArray orders = jsonObject.getAsJsonArray("orders");
        for (JsonElement orderElement : orders) {
            JsonObject orderObject = orderElement.getAsJsonObject();
            purchases.add(new Purchase(
                    PurchaseState.valueOf(orderObject.get("purchaseState").getAsInt()),
                    orderObject.has("notificationId") ? orderObject.get("notificationId").getAsString() : null,
                    orderObject.get("productId").getAsString()));
        }
        return purchases;
    }
}
