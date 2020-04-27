package org.tasks.activities.attribution;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import org.tasks.R;

class LicenseRow extends RecyclerView.ViewHolder {

  @BindView(R.id.copyright_holder)
  TextView copyrightHolder;

  @BindView(R.id.libraries)
  TextView libraries;

  LicenseRow(@NonNull View itemView) {
    super(itemView);
    ButterKnife.bind(this, itemView);
  }

  void bind(String copyrightHolder, String libraries) {
    this.copyrightHolder.setText(copyrightHolder);
    this.libraries.setText(libraries);
  }
}