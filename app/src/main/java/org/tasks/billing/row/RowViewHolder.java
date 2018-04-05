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
  public final Button button;

  public interface ButtonClick {
    void onClick(int row);
  }

  public RowViewHolder(final View itemView, final ButtonClick onClick) {
    super(itemView);
    title = itemView.findViewById(R.id.title);
    price = itemView.findViewById(R.id.price);
    description = itemView.findViewById(R.id.description);
    button = itemView.findViewById(R.id.buy_button);
    if (button != null) {
      button.setOnClickListener(view -> onClick.onClick(getAdapterPosition()));
    }
  }
}
