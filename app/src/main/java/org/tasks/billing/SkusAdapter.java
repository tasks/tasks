/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tasks.billing;

import static com.google.common.collect.Lists.transform;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static java.util.Arrays.asList;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.billingclient.api.BillingClient.SkuType;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.lang.annotation.Retention;
import java.util.List;
import javax.annotation.Nonnull;
import org.tasks.BuildConfig;
import org.tasks.R;
import org.tasks.billing.row.RowDataProvider;
import org.tasks.billing.row.RowViewHolder;
import org.tasks.billing.row.RowViewHolder.ButtonClick;
import org.tasks.billing.row.SkuRowData;

public class SkusAdapter extends RecyclerView.Adapter<RowViewHolder>
    implements RowDataProvider, ButtonClick {

  public static final int TYPE_HEADER = 0;
  public static final int TYPE_NORMAL = 1;
  private final Context context;
  private final Inventory inventory;
  private final OnClickHandler onClickHandler;
  private List<SkuRowData> data = ImmutableList.of();

  SkusAdapter(Context context, Inventory inventory, OnClickHandler onClickHandler) {
    this.context = context;
    this.inventory = inventory;
    this.onClickHandler = onClickHandler;
  }

  public void setData(List<SkuRowData> data) {
    this.data = data;

    notifyDataSetChanged();
  }

  @Override
  public @RowTypeDef int getItemViewType(int position) {
    return data.isEmpty() ? TYPE_HEADER : data.get(position).getRowType();
  }

  @Override
  @Nonnull
  public RowViewHolder onCreateViewHolder(@Nonnull ViewGroup parent, @RowTypeDef int viewType) {
    // Selecting a flat layout for header rows
    if (viewType == SkusAdapter.TYPE_HEADER) {
      View item =
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.sku_details_row_header, parent, false);
      return new RowViewHolder(item, null);
    } else {
      View item =
          LayoutInflater.from(parent.getContext()).inflate(R.layout.sku_details_row, parent, false);
      return new RowViewHolder(item, this);
    }
  }

  @Override
  public void onBindViewHolder(@Nonnull RowViewHolder holder, int position) {
    SkuRowData data = getData(position);
    if (data != null) {
      holder.title.setText(data.getTitle());
      if (getItemViewType(position) != SkusAdapter.TYPE_HEADER) {

        String sku = data.getSku();
        if (SkuType.SUBS.equals(data.getSkuType())) {
          String[] rows = context.getResources().getStringArray(R.array.pro_description);
          holder.description.setText(
              Joiner.on('\n').join(transform(asList(rows), item -> "\u2022 " + item)));
          holder.subscribeButton.setVisibility(View.VISIBLE);
          holder.price.setVisibility(View.VISIBLE);
          holder.price.setText(data.getPrice());
          if (inventory.purchased(sku)) {
            holder.subscribeButton.setText(R.string.button_subscribed);
            holder.auxiliaryButton.setVisibility(View.GONE);
          } else {
            holder.subscribeButton.setText(R.string.button_subscribe);
            holder.auxiliaryButton.setVisibility(View.VISIBLE);
          }
        } else {
          holder.description.setText(data.getDescription());
          holder.subscribeButton.setVisibility(View.GONE);
          holder.price.setVisibility(View.GONE);
          holder.auxiliaryButton.setVisibility(View.GONE);
          if (BuildConfig.DEBUG) {
            holder.subscribeButton.setVisibility(View.VISIBLE);
            holder.subscribeButton.setText(
                inventory.purchased(sku) ? R.string.debug_consume : R.string.debug_buy);
          }
        }
      }
    }
  }

  @Override
  public int getItemCount() {
    return data.size();
  }

  @Override
  public SkuRowData getData(int position) {
    return data.isEmpty() ? null : data.get(position);
  }

  @Override
  public void onAuxiliaryClick(int row) {
    onClickHandler.clickAux(getData(row));
  }

  @Override
  public void onClick(int row) {
    onClickHandler.click(getData(row));
  }

  public interface OnClickHandler {
    void clickAux(SkuRowData skuRowData);

    void click(SkuRowData skuRowData);
  }

  /** Types for adapter rows */
  @Retention(SOURCE)
  @IntDef({TYPE_HEADER, TYPE_NORMAL})
  public @interface RowTypeDef {}
}
