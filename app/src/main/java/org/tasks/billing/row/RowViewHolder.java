package org.tasks.billing.row;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.tasks.R;

public final class RowViewHolder extends RecyclerView.ViewHolder {
  public final TextView title;
  public final TextView description;
  public final TextView price;
  public final Button subscribeButton;
  public final Button auxiliaryButton;

  public RowViewHolder(final View itemView, final ButtonClick onClick) {
    super(itemView);
    title = itemView.findViewById(R.id.title);
    price = itemView.findViewById(R.id.price);
    description = itemView.findViewById(R.id.description);
    subscribeButton = itemView.findViewById(R.id.buy_button);
    auxiliaryButton = itemView.findViewById(R.id.aux_button);
    if (auxiliaryButton != null) {
      auxiliaryButton.setOnClickListener(view -> onClick.onAuxiliaryClick(getAdapterPosition()));
    }
    if (subscribeButton != null) {
      subscribeButton.setOnClickListener(view -> onClick.onClick(getAdapterPosition()));
    }
  }

  public interface ButtonClick {
    void onAuxiliaryClick(int row);

    void onClick(int row);
  }
}
