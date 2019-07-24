package org.tasks.billing;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.button.MaterialButton;
import org.tasks.Callback;
import org.tasks.R;
import org.tasks.locale.Locale;

public class PurchaseHolder extends RecyclerView.ViewHolder {

  private final Callback<Integer> onClick;
  private final Locale locale;

  @BindView(R.id.price)
  MaterialButton button;

  private int price;

  PurchaseHolder(@NonNull View view, Callback<Integer> onClick, Locale locale) {
    super(view);
    this.locale = locale;

    ButterKnife.bind(this, view);

    this.onClick = onClick;
  }

  @OnClick(R.id.price)
  void onClick() {
    onClick.call(price);
  }

  public void bind(int price, boolean selected) {
    this.price = price;
    button.setText(String.format("$%s", locale.formatNumber(price)));
    button.setChecked(selected);
  }
}
