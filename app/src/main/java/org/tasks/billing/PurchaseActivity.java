package org.tasks.billing;

import static org.tasks.billing.PurchaseDialog.newPurchaseDialog;

import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import javax.inject.Inject;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.InjectingAppCompatActivity;
import org.tasks.themes.ThemeAccent;

public class PurchaseActivity extends InjectingAppCompatActivity {

  private static final String FRAG_TAG_PURCHASE = "frag_tag_purchase";

  @Inject Inventory inventory;
  @Inject ThemeAccent themeAccent;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    themeAccent.applyStyle(getTheme());

    FragmentManager fragmentManager = getSupportFragmentManager();
    PurchaseDialog dialog = (PurchaseDialog) fragmentManager.findFragmentByTag(FRAG_TAG_PURCHASE);
    if (dialog == null) {
      dialog = newPurchaseDialog();
      dialog.show(fragmentManager, FRAG_TAG_PURCHASE);
    }
    dialog.setOnDismissListener(d -> finish());
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
