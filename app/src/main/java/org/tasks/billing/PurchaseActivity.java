package org.tasks.billing;

import static android.text.TextUtils.isEmpty;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static org.tasks.billing.Inventory.SKU_DASHCLOCK;
import static org.tasks.billing.Inventory.SKU_TASKER;
import static org.tasks.billing.Inventory.SKU_THEMES;
import static org.tasks.billing.Inventory.SKU_VIP;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.BuildConfig;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.billing.SkusAdapter.OnClickHandler;
import org.tasks.billing.row.SkuRowData;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.ui.MenuColorizer;
import timber.log.Timber;

public class PurchaseActivity extends ThemedInjectingAppCompatActivity
    implements OnClickHandler, OnMenuItemClickListener {

  private static final List<String> DEBUG_SKUS =
      ImmutableList.of(SKU_THEMES, SKU_TASKER, SKU_DASHCLOCK, SKU_VIP);

  @Inject @ForApplication Context context;
  @Inject BillingClient billingClient;
  @Inject Inventory inventory;
  @Inject LocalBroadcastManager localBroadcastManager;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.list)
  RecyclerView recyclerView;

  @BindView(R.id.screen_wait)
  View loadingView;

  @BindView(R.id.error_textview)
  TextView errorTextView;

  private SkusAdapter adapter;
  private BroadcastReceiver purchaseReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          querySkuDetails();
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_purchase);

    ButterKnife.bind(this);

    toolbar.setTitle(R.string.upgrade);
    toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
    toolbar.setNavigationOnClickListener(v -> onBackPressed());
    toolbar.inflateMenu(R.menu.menu_purchase_activity);
    toolbar.setOnMenuItemClickListener(this);
    MenuColorizer.colorToolbar(this, toolbar);

    adapter = new SkusAdapter(context, inventory, this);
    recyclerView.setAdapter(adapter);
    Resources res = getResources();
    recyclerView.addItemDecoration(
        new CardsWithHeadersDecoration(
            adapter,
            (int) res.getDimension(R.dimen.header_gap),
            (int) res.getDimension(R.dimen.row_gap)));
    recyclerView.setLayoutManager(new LinearLayoutManager(context));
    setWaitScreen(true);
    querySkuDetails();
  }

  @Override
  protected void onResume() {
    super.onResume();

    querySkuDetails();
  }

  @Override
  protected void onStart() {
    super.onStart();

    localBroadcastManager.registerPurchaseReceiver(purchaseReceiver);
  }

  @Override
  protected void onStop() {
    super.onStop();

    localBroadcastManager.unregisterReceiver(purchaseReceiver);
  }

  /** Queries for in-app and subscriptions SKU details and updates an adapter with new data */
  private void querySkuDetails() {
    if (!isFinishing()) {
      List<SkuRowData> data = new ArrayList<>();
      String owned = getString(R.string.owned);
      String debug = getString(R.string.debug);
      Runnable addDebug =
          BuildConfig.DEBUG
              ? () ->
                  addSkuRows(
                      data,
                      newArrayList(
                          filter(DEBUG_SKUS, sku -> !any(data, row -> sku.equals(row.getSku())))),
                      debug,
                      SkuType.INAPP,
                      null)
              : null;
      Runnable addIaps =
          () ->
              addSkuRows(
                  data,
                  newArrayList(
                      filter(
                          transform(inventory.getPurchases(), Purchase::getSku),
                          sku1 -> !Inventory.SKU_SUBS.contains(sku1))),
                  owned,
                  SkuType.INAPP,
                  addDebug);
      addSkuRows(data, Inventory.SKU_SUBS, null, SkuType.SUBS, addIaps);
    }
  }

  private void addSkuRows(
      List<SkuRowData> data,
      List<String> skus,
      String title,
      @SkuType String skuType,
      Runnable whenFinished) {
    billingClient.querySkuDetailsAsync(
        skuType,
        skus,
        (responseCode, skuDetailsList) -> {
          if (responseCode != BillingResponse.OK) {
            Timber.w("Unsuccessful query for type: " + skuType + ". Error code: " + responseCode);
          } else if (skuDetailsList != null && skuDetailsList.size() > 0) {
            if (!isEmpty(title)) {
              data.add(new SkuRowData(title));
            }
            Timber.d("Adding %s skus", skuDetailsList.size());
            // Then fill all the other rows
            for (SkuDetails details : skuDetailsList) {
              Timber.i("Adding sku: %s", details);
              data.add(new SkuRowData(details, SkusAdapter.TYPE_NORMAL, skuType));
            }

            if (data.size() == 0) {
              displayAnErrorIfNeeded();
            } else {
              adapter.setData(data);
              setWaitScreen(false);
            }
          }

          if (whenFinished != null) {
            whenFinished.run();
          }
        });
  }

  private void displayAnErrorIfNeeded() {
    if (!isFinishing()) {
      loadingView.setVisibility(View.GONE);
      errorTextView.setVisibility(View.VISIBLE);
      errorTextView.setText(
          billingClient.getBillingClientResponseCode() == BillingResponse.BILLING_UNAVAILABLE
              ? R.string.error_billing_unavailable
              : R.string.error_billing_default);
    }
  }

  private void setWaitScreen(boolean set) {
    recyclerView.setVisibility(set ? View.GONE : View.VISIBLE);
    loadingView.setVisibility(set ? View.VISIBLE : View.GONE);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public void clickAux(SkuRowData skuRowData) {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://tasks.org/subscribe")));
  }

  @Override
  public void click(SkuRowData skuRowData) {
    String sku = skuRowData.getSku();
    String skuType = skuRowData.getSkuType();
    if (inventory.purchased(sku)) {
      if (BuildConfig.DEBUG && SkuType.INAPP.equals(skuType)) {
        billingClient.consume(sku);
      }
    } else {
      billingClient.initiatePurchaseFlow(this, sku, skuType);
    }
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_help:
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://tasks.org/subscribe")));
        return true;
      case R.id.menu_refresh_purchases:
        billingClient.queryPurchases();
        return true;
      default:
        return false;
    }
  }
}
