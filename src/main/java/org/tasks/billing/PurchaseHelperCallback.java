package org.tasks.billing;

public interface PurchaseHelperCallback {
    void purchaseCompleted(boolean success, String sku);
}
