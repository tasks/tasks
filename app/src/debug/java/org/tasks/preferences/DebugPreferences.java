package org.tasks.preferences;

import static com.google.common.primitives.Ints.asList;

import android.os.Bundle;
import android.preference.Preference;
import androidx.annotation.StringRes;
import at.bitfire.cert4android.CustomCertManager;
import com.android.billingclient.api.BillingClient.SkuType;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.billing.BillingClient;
import org.tasks.billing.Inventory;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingPreferenceActivity;
import org.tasks.ui.Toaster;

public class DebugPreferences extends InjectingPreferenceActivity {

  @Inject Inventory inventory;
  @Inject BillingClient billingClient;
  @Inject Toaster toaster;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences_debug);

    for (int pref :
        asList(
            R.string.p_leakcanary,
            R.string.p_flipper,
            R.string.p_strict_mode_vm,
            R.string.p_strict_mode_thread)) {
      findPreference(pref)
          .setOnPreferenceChangeListener(
              (preference, newValue) -> {
                showRestartDialog();
                return true;
              });
    }

    findPreference(R.string.debug_reset_ssl)
        .setOnPreferenceClickListener(
            preference -> {
              CustomCertManager.Companion.resetCertificates(this);
              toaster.longToast("SSL certificates reset");
              return false;
            });

    setupIap(R.string.debug_themes, Inventory.SKU_THEMES);
    setupIap(R.string.debug_tasker, Inventory.SKU_TASKER);
    setupIap(R.string.debug_dashclock, Inventory.SKU_DASHCLOCK);
  }

  private void setupIap(@StringRes int prefId, String sku) {
    Preference preference = findPreference(prefId);
    if (inventory.getPurchase(sku) == null) {
      preference.setTitle(getString(R.string.debug_purchase, sku));
      preference.setOnPreferenceClickListener(
          p -> {
            billingClient.initiatePurchaseFlow(DebugPreferences.this, sku, SkuType.INAPP, null);
            return false;
          });
    } else {
      preference.setTitle(getString(R.string.debug_consume, sku));
      preference.setOnPreferenceClickListener(
          p -> {
            billingClient.consume(sku);
            return false;
          });
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
