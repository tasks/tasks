package org.tasks.billing;

import static org.tasks.billing.NameYourPriceDialog.newNameYourPriceDialog;
import static org.tasks.billing.PurchaseDialog.newPurchaseDialog;

import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import javax.inject.Inject;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;

public class PurchaseActivity extends ThemedInjectingAppCompatActivity {

  private static final String FRAG_TAG_PURCHASE = "frag_tag_purchase";
  private static final String FRAG_TAG_PRICE = "frag_tag_price";

  @Inject Inventory inventory;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    FragmentManager fragmentManager = getSupportFragmentManager();
    if (inventory.hasPro()) {
      NameYourPriceDialog dialog = (NameYourPriceDialog) fragmentManager.findFragmentByTag(FRAG_TAG_PRICE);
      if (dialog == null) {
        dialog = newNameYourPriceDialog();
        dialog.show(fragmentManager, FRAG_TAG_PRICE);
      }
      dialog.setOnDismissListener(d -> finish());
    } else {
      PurchaseDialog dialog = (PurchaseDialog) fragmentManager.findFragmentByTag(FRAG_TAG_PURCHASE);
      if (dialog == null) {
        dialog = newPurchaseDialog();
        dialog.show(fragmentManager, FRAG_TAG_PURCHASE);
      }
      dialog.setOnDismissListener(d -> finish());
    }
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
