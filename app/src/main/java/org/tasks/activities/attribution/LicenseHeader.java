package org.tasks.activities.attribution;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import org.tasks.R;

class LicenseHeader extends RecyclerView.ViewHolder {
  @BindView(R.id.license_name)
  TextView licenseName;

  LicenseHeader(@NonNull View itemView) {
    super(itemView);
    ButterKnife.bind(this, itemView);
  }

  void bind(String license) {
    licenseName.setText(license);
  }
}
