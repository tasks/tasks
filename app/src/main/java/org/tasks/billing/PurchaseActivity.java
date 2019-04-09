package org.tasks.billing;

import static com.google.common.collect.Lists.transform;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.ArrayList;
import java.util.Collections;
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

public class PurchaseActivity extends ThemedInjectingAppCompatActivity
    implements OnClickHandler, OnMenuItemClickListener {

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
          billingClient.querySkuDetails();
        }
      };
  private List<SkuDetails> iaps = Collections.emptyList();
  private List<SkuDetails> subscriptions = Collections.emptyList();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_purchase);

    ButterKnife.bind(this);

    toolbar.setTitle(R.string.upgrade);
    toolbar.setNavigationIcon(R.drawable.ic_outline_arrow_back_24px);
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
  }

  @Override
  protected void onResume() {
    super.onResume();

    billingClient.observeSkuDetails(this, this::onSubscriptionsUpdated, this::onIapsUpdated);
    billingClient.querySkuDetails();
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

  private void onIapsUpdated(List<SkuDetails> iaps) {
    this.iaps = iaps;
    updateSkuDetails();
  }

  private void onSubscriptionsUpdated(List<SkuDetails> subscriptions) {
    this.subscriptions = subscriptions;
    updateSkuDetails();
  }

  private void updateSkuDetails() {
    List<SkuRowData> data = new ArrayList<>(transform(subscriptions, SkuRowData::new));
    if (iaps.size() > 0) {
      data.add(new SkuRowData(context.getString(R.string.owned)));
      data.addAll(transform(iaps, SkuRowData::new));
    }
    if (data.isEmpty()) {
      displayAnErrorIfNeeded();
    } else {
      adapter.setData(data);
      setWaitScreen(false);
    }
  }

  private void displayAnErrorIfNeeded() {
    if (!isFinishing()) {
      loadingView.setVisibility(View.GONE);
      errorTextView.setVisibility(View.VISIBLE);
      errorTextView.setText(billingClient.getErrorMessage());
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
    startSubscribeActivity();
  }

  @Override
  public void click(SkuRowData skuRowData) {
    String sku = skuRowData.getSku();
    String skuType = skuRowData.getSkuType();
    if (inventory.purchased(sku)) {
      if (BuildConfig.DEBUG && SkuDetails.TYPE_INAPP.equals(skuType)) {
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
        startSubscribeActivity();
        return true;
      case R.id.menu_refresh_purchases:
        billingClient.queryPurchases();
        return true;
      default:
        return false;
    }
  }

  private void startSubscribeActivity() {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://tasks.org/subscribe")));
  }
}
