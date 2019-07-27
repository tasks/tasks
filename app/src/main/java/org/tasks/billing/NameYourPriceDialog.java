package org.tasks.billing;

import static com.google.common.collect.Lists.newArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import javax.inject.Inject;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.dialogs.DialogBuilder;
import org.tasks.dialogs.IconLayoutManager;
import org.tasks.injection.DialogFragmentComponent;
import org.tasks.injection.ForActivity;
import org.tasks.injection.InjectingDialogFragment;
import org.tasks.locale.Locale;
import org.tasks.themes.Theme;

public class NameYourPriceDialog extends InjectingDialogFragment implements OnPurchasesUpdated {

  private static final String EXTRA_MONTHLY = "extra_monthly";
  private static final String EXTRA_PRICE = "extra_price";

  @Inject DialogBuilder dialogBuilder;
  @Inject @ForActivity Context context;
  @Inject BillingClient billingClient;
  @Inject LocalBroadcastManager localBroadcastManager;
  @Inject Inventory inventory;
  @Inject Locale locale;
  @Inject Theme theme;

  @BindView(R.id.recycler_view)
  RecyclerView recyclerView;

  @BindView(R.id.screen_wait)
  View loadingView;

  @BindView(R.id.buttons)
  MaterialButtonToggleGroup buttons;

  @BindView(R.id.subscribe)
  MaterialButton subscribe;

  @BindView(R.id.unsubscribe)
  MaterialButton unsubscribe;

  private PurchaseAdapter adapter;
  private Purchase currentSubscription = null;
  private BroadcastReceiver purchaseReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          setup();
        }
      };
  private OnDismissListener listener;

  static NameYourPriceDialog newNameYourPriceDialog() {
    return new NameYourPriceDialog();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    View view = theme.getLayoutInflater(context).inflate(R.layout.dialog_name_your_price, null);

    ButterKnife.bind(this, view);

    setWaitScreen(true);

    adapter = new PurchaseAdapter(context, theme, locale, this::onPriceChanged);

    buttons.addOnButtonCheckedListener(this::onButtonChecked);

    if (savedInstanceState != null) {
      buttons.check(
          savedInstanceState.getBoolean(EXTRA_MONTHLY)
              ? R.id.button_monthly
              : R.id.button_annually);
      adapter.setSelected(savedInstanceState.getInt(EXTRA_PRICE));
    }

    return dialogBuilder.newDialog(R.string.name_your_price).setView(view).show();
  }

  private void onButtonChecked(MaterialButtonToggleGroup group, int id, boolean checked) {
    if (id == R.id.button_monthly) {
      if (!checked && group.getCheckedButtonId() != R.id.button_annually) {
        group.check(R.id.button_monthly);
      }
    } else {
      if (!checked && group.getCheckedButtonId() != R.id.button_monthly) {
        group.check(R.id.button_annually);
      }
    }
    updateSubscribeButton();
  }

  private boolean isMonthly() {
    return buttons.getCheckedButtonId() == R.id.button_monthly;
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putBoolean(EXTRA_MONTHLY, isMonthly());
    outState.putInt(EXTRA_PRICE, adapter.getSelected());
  }

  @SuppressLint("DefaultLocale")
  @OnClick(R.id.subscribe)
  protected void subscribe() {
    if (currentSubscriptionSelected() && currentSubscription.isCanceled()) {
      billingClient.initiatePurchaseFlow(
          (Activity) context, currentSubscription.getSku(), SkuDetails.TYPE_SUBS, null);
    } else {
      billingClient.initiatePurchaseFlow(
          (Activity) context,
          String.format("%s_%02d", isMonthly() ? "monthly" : "annual", adapter.getSelected()),
          SkuDetails.TYPE_SUBS,
          currentSubscription == null ? null : currentSubscription.getSku());
    }
    billingClient.addPurchaseCallback(this);
    dismiss();
  }

  private void setup() {
    currentSubscription = inventory.getSubscription();
    if (adapter.getSelected() == 0) {
      if (currentSubscription == null) {
        adapter.setSelected(1);
      } else {
        adapter.setSelected(currentSubscription.getSubscriptionPrice());
        buttons.check(currentSubscription.isMonthly() ? R.id.button_monthly : R.id.button_annually);
      }
    }
    unsubscribe.setVisibility(
        currentSubscription == null || currentSubscription.isCanceled() ? View.GONE : View.VISIBLE);
    updateSubscribeButton();
    setWaitScreen(false);
    adapter.submitList(
        newArrayList(ContiguousSet.create(Range.closed(1, 10), DiscreteDomain.integers())));
    recyclerView.setLayoutManager(new IconLayoutManager(context));
    recyclerView.setAdapter(adapter);
  }

  @OnClick(R.id.unsubscribe)
  protected void manageSubscription() {
    startActivity(
        new Intent(
            Intent.ACTION_VIEW,
            Uri.parse(getString(R.string.manage_subscription_url, currentSubscription.getSku()))));
    dismiss();
  }

  private void onPriceChanged(Integer price) {
    adapter.setSelected(price);
    updateSubscribeButton();
  }

  private void updateSubscribeButton() {
    subscribe.setEnabled(true);
    if (currentSubscription == null) {
      subscribe.setText(R.string.button_subscribe);
    } else if (currentSubscriptionSelected()) {
      if (currentSubscription.isCanceled()) {
        subscribe.setText(R.string.button_restore_subscription);
      } else {
        subscribe.setText(R.string.button_current_subscription);
        subscribe.setEnabled(false);
      }
    } else {
      subscribe.setText(isUpgrade() ? R.string.button_upgrade : R.string.button_downgrade);
    }
  }

  private boolean isUpgrade() {
    return isMonthly() == currentSubscription.isMonthly()
        ? currentSubscription.getSubscriptionPrice() < adapter.getSelected()
        : isMonthly();
  }

  private boolean currentSubscriptionSelected() {
    return currentSubscription != null
        && isMonthly() == currentSubscription.isMonthly()
        && adapter.getSelected() == currentSubscription.getSubscriptionPrice();
  }

  @Override
  protected void inject(DialogFragmentComponent component) {
    component.inject(this);
  }

  private void setWaitScreen(boolean isWaitScreen) {
    recyclerView.setVisibility(isWaitScreen ? View.GONE : View.VISIBLE);
    buttons.setVisibility(isWaitScreen ? View.GONE : View.VISIBLE);
    subscribe.setVisibility(isWaitScreen ? View.GONE : View.VISIBLE);
    loadingView.setVisibility(isWaitScreen ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onStart() {
    super.onStart();

    localBroadcastManager.registerPurchaseReceiver(purchaseReceiver);
    billingClient.queryPurchases();
  }

  @Override
  public void onStop() {
    super.onStop();

    localBroadcastManager.unregisterReceiver(purchaseReceiver);
  }

  NameYourPriceDialog setOnDismissListener(OnDismissListener listener) {
    this.listener = listener;
    return this;
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    super.onDismiss(dialog);

    if (listener != null) {
      listener.onDismiss(dialog);
    }
  }

  @Override
  public void onPurchasesUpdated() {
    dismiss();
  }

  @OnClick(R.id.button_more_info)
  public void openDocumentation() {
    startActivity(
        new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.subscription_help_url))));
    dismiss();
  }
}
