package org.tasks.billing;

import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductType;

public class SkuDetails {

  static final String SKU_PRO = "tasks_pro";

  static final String TYPE_INAPP = ProductType.CONSUMABLE.name();
  static final String TYPE_SUBS = ProductType.SUBSCRIPTION.name();

  private final Product product;

  public SkuDetails(Product product) {
    this.product = product;
  }

  public String getSku() {
    return product.getSku();
  }

  public String getTitle() {
    return product.getTitle();
  }

  public String getPrice() {
    return product.getPrice();
  }

  public String getDescription() {
    return product.getDescription();
  }

  public String getSkuType() {
    return product.getProductType().name();
  }
}
