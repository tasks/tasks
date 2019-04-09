package org.tasks.billing;

import com.amazon.device.iap.model.Receipt;
import com.google.gson.GsonBuilder;

public class Purchase {

  private final Receipt receipt;

  public Purchase(String json) {
    this(new GsonBuilder().create().fromJson(json, Receipt.class));
  }

  public Purchase(Receipt receipt) {
    this.receipt = receipt;
  }

  public String getSku() {
    return receipt.getSku();
  }

  public String toJson() {
    return new GsonBuilder().create().toJson(receipt);
  }

  @Override
  public String toString() {
    return "Purchase{" + "receipt=" + receipt + '}';
  }
}
