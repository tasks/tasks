// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.tasks.billing;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import org.tasks.billing.row.RowDataProvider;
import org.tasks.billing.row.SkuRowData;

/** A separator for RecyclerView that keeps the specified spaces between headers and the cards. */
public class CardsWithHeadersDecoration extends RecyclerView.ItemDecoration {

  private final RowDataProvider mRowDataProvider;
  private final int mHeaderGap, mRowGap;

  public CardsWithHeadersDecoration(RowDataProvider rowDataProvider, int headerGap, int rowGap) {
    this.mRowDataProvider = rowDataProvider;
    this.mHeaderGap = headerGap;
    this.mRowGap = rowGap;
  }

  @Override
  public void getItemOffsets(
      Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

    final int position = parent.getChildAdapterPosition(view);
    final SkuRowData data = mRowDataProvider.getData(position);

    // We should add a space on top of every header card
    if (data.getRowType() == SkusAdapter.TYPE_HEADER || position == 0) {
      outRect.top = mHeaderGap;
    }

    // Adding a space under the last item
    if (position == parent.getAdapter().getItemCount() - 1) {
      outRect.bottom = mHeaderGap;
    } else {
      outRect.bottom = mRowGap;
    }
  }
}
