package org.tasks.activities.attribution;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import java.util.List;
import org.tasks.R;

public class AttributionAdapter extends RecyclerView.Adapter<ViewHolder> {

  private final List<AttributionRow> rows;

  AttributionAdapter(List<AttributionRow> rows) {
    this.rows = rows;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    return viewType == 0
        ? new LicenseHeader(inflater.inflate(R.layout.row_attribution_header, parent, false))
        : new LicenseRow(inflater.inflate(R.layout.row_attribution, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    AttributionRow row = rows.get(position);
    if (getItemViewType(position) == 0) {
      ((LicenseHeader) holder).bind(row.getLicense());
    } else {
      ((LicenseRow) holder).bind(row.getCopyrightHolder(), row.getLibraries());
    }
  }

  @Override
  public int getItemViewType(int position) {
    return rows.get(position).isHeader() ? 0 : 1;
  }

  @Override
  public int getItemCount() {
    return rows.size();
  }
}
