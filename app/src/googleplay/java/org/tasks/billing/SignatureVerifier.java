package org.tasks.billing;

import android.content.Context;
import java.io.IOException;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ForApplication;
import timber.log.Timber;

class SignatureVerifier {

  private final String billingKey;

  @Inject
  SignatureVerifier(@ForApplication Context context) {
    billingKey = context.getString(R.string.gp_key);
  }

  boolean verifySignature(Purchase purchase) {
    try {
      return Security.verifyPurchase(
          billingKey, purchase.getOriginalJson(), purchase.getSignature());
    } catch (IOException e) {
      Timber.e(e);
      return false;
    }
  }
}
