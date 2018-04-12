package org.tasks.billing;

import android.content.Context;
import com.android.billingclient.api.Purchase;
import java.io.IOException;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import timber.log.Timber;

public class SignatureVerifier {

  private final String billingKey;

  @Inject
  public SignatureVerifier(@ForApplication Context context) {
    billingKey = context.getString(R.string.gp_key);
  }

  public boolean verifySignature(Purchase purchase) {
    try {
      return Security.verifyPurchase(
          billingKey, purchase.getOriginalJson(), purchase.getSignature());
    } catch (IOException e) {
      Timber.e(e);
      return false;
    }
  }
}
