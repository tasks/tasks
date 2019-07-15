package org.tasks.billing;

import static org.tasks.preferences.ResourceResolver.getData;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.tasks.Callback;
import org.tasks.R;
import org.tasks.locale.Locale;

public class PurchaseHolder extends RecyclerView.ViewHolder {

  private final Context context;
  private final Callback<Integer> onClick;
  private final Locale locale;

  @BindView(R.id.price)
  TextView textView;

  private int price;

  PurchaseHolder(Context context, @NonNull View view, Callback<Integer> onClick, Locale locale) {
    super(view);
    this.locale = locale;

    ButterKnife.bind(this, view);

    this.context = context;
    this.onClick = onClick;
  }

  @OnClick(R.id.price)
  void onClick() {
    onClick.call(price);
  }

  public void bind(int price, boolean selected) {
    this.price = price;
    textView.setText(String.format("$%s", locale.formatNumber(price)));
    textView.setTextColor(
        selected
            ? getData(context, R.attr.colorPrimary)
            : ContextCompat.getColor(context, R.color.text_primary));
  }
}
