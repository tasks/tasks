package org.tasks.billing;

public interface PurchaseHelperCallback {

    PurchaseHelperCallback NO_OP = new PurchaseHelperCallback() {
        @Override
        public void purchaseCompleted(boolean success, String sku) {

        }
    };

    void purchaseCompleted(boolean success, String sku);
}
