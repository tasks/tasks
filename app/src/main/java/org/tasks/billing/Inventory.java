package org.tasks.billing;

import android.content.Context;
import com.android.billingclient.api.Purchase;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ApplicationScope;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

@ApplicationScope
public class Inventory {

  private static final String SKU_PRO = "annual_499";
  static final String SKU_VIP = "vip";
  static final String SKU_TASKER = "tasker";
  static final String SKU_THEMES = "themes";
  static final String SKU_DASHCLOCK = "dashclock";

  public static final List<String> SKU_SUBS = ImmutableList.of(SKU_PRO);

  private final Preferences preferences;
  private final String billingKey;

  private Map<String, Purchase> purchases = new HashMap<>();

  @Inject
  public Inventory(@ForApplication Context context, Preferences preferences) {
    this.preferences = preferences;
    billingKey = context.getString(R.string.gp_key);
    for (Purchase purchase : preferences.getPurchases()) {
      add(purchase);
    }
  }

  public void clear() {
    Timber.d("clear()");
    purchases.clear();
  }

  public void add(List<Purchase> purchases) {
    for (Purchase purchase : purchases) {
      add(purchase);
    }
    preferences.setPurchases(this.purchases.values());
  }

  private void add(Purchase purchase) {
    if (verifySignature(purchase)) {
      Timber.d("add(%s)", purchase);
      purchases.put(purchase.getSku(), purchase);
    }
  }

  public boolean purchasedTasker() {
    return hasPro() || purchases.containsKey(SKU_TASKER);
  }

  public boolean purchasedDashclock() {
    return hasPro() || purchases.containsKey(SKU_DASHCLOCK);
  }

  public boolean purchasedThemes() {
    return hasPro() || purchases.containsKey(SKU_THEMES);
  }

  public List<Purchase> getPurchases() {
    return ImmutableList.copyOf(purchases.values());
  }

  public boolean hasPro() {
    return purchases.containsKey(SKU_PRO) || purchases.containsKey(SKU_VIP);
  }

  public boolean purchased(String sku) {
    return purchases.containsKey(sku);
  }

  private boolean verifySignature(Purchase purchase) {
    try {
      return Security.verifyPurchase(
          billingKey, purchase.getOriginalJson(), purchase.getSignature());
    } catch (IOException e) {
      Timber.e(e);
      return false;
    }
  }

  public Purchase getPurchase(String sku) {
    return purchases.get(sku);
  }
}
