package org.tasks.billing;

import static java.util.Collections.singletonList;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.injection.ApplicationScope;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

@ApplicationScope
public class Inventory {

  static final String SKU_VIP = "vip";
  static final String SKU_TASKER = "tasker";
  static final String SKU_THEMES = "themes";
  static final String SKU_DASHCLOCK = "dashclock";

  private final Preferences preferences;
  private final SignatureVerifier signatureVerifier;
  private final LocalBroadcastManager localBroadcastManager;

  private Map<String, Purchase> purchases = new HashMap<>();

  @Inject
  public Inventory(
      Preferences preferences,
      SignatureVerifier signatureVerifier,
      LocalBroadcastManager localBroadcastManager) {
    this.preferences = preferences;
    this.signatureVerifier = signatureVerifier;
    this.localBroadcastManager = localBroadcastManager;
    for (Purchase purchase : preferences.getPurchases()) {
      verifyAndAdd(purchase);
    }
  }

  public void clear() {
    Timber.d("clear()");
    purchases.clear();
  }

  public void add(Purchase purchase) {
    add(singletonList(purchase));
  }

  public void add(Iterable<Purchase> purchases) {
    for (Purchase purchase : purchases) {
      verifyAndAdd(purchase);
    }
    preferences.setPurchases(this.purchases.values());
    localBroadcastManager.broadcastPurchasesUpdated();
  }

  private void verifyAndAdd(Purchase purchase) {
    if (signatureVerifier.verifySignature(purchase)) {
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
    //noinspection ConstantConditions
    return purchases.containsKey(SkuDetails.SKU_PRO)
        || purchases.containsKey(SKU_VIP)
        || BuildConfig.FLAVOR.equals("generic")
        || (BuildConfig.DEBUG && preferences.getBoolean(R.string.p_debug_pro, false));
  }

  public boolean purchased(String sku) {
    return purchases.containsKey(sku);
  }

  public Purchase getPurchase(String sku) {
    return purchases.get(sku);
  }
}
