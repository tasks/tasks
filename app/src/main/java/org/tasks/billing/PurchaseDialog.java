package org.tasks.billing;

import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;
import static org.tasks.billing.NameYourPriceDialog.newNameYourPriceDialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.base.Joiner;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.themes.Theme;

public class PurchaseDialog extends InjectingDialogFragment {

  private static final String FRAG_TAG_PRICE = "frag_tag_price";

  @Inject DialogBuilder dialogBuilder;
  @Inject Theme theme;
  @Inject @ForActivity Context context;
  private OnDismissListener listener;

  public static PurchaseDialog newPurchaseDialog() {
    return new PurchaseDialog();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    View view = theme.getLayoutInflater(context).inflate(R.layout.dialog_purchase, null);
    TextView textView = view.findViewById(R.id.feature_list);
    String[] rows = context.getResources().getStringArray(R.array.pro_description);
    textView.setText(Joiner.on('\n').join(transform(asList(rows), item -> "\u2022 " + item)));
    return dialogBuilder
        .newDialog(R.string.pro_support_development)
        .setView(view)
        .setPositiveButton(
            R.string.name_your_price,
            (dialog, which) -> {
              newNameYourPriceDialog()
                  .setOnDismissListener(listener)
                  .show(getFragmentManager(), FRAG_TAG_PRICE);
              listener = null;
              dialog.dismiss();
            })
        .show();
  }

  void setOnDismissListener(OnDismissListener listener) {
    this.listener = listener;
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    super.onDismiss(dialog);

    if (listener != null) {
      listener.onDismiss(dialog);
    }
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }
}
