package org.tasks.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.List;
import org.tasks.R;

public class HiddenTopArrayAdapter<T> extends ArrayAdapter<T> {

  protected HiddenTopArrayAdapter(Context context, int resources, List<T> objects) {
    super(context, resources, objects);
  }

  @Override
  public View getDropDownView(
      final int position, final View convertView, @NonNull final ViewGroup parent) {
    View v;

    if (position == 0) {
      TextView tv = new TextView(getContext());
      tv.setHeight(0);
      tv.setVisibility(View.GONE);
      v = tv;
    } else {
      ViewGroup vg =
          (ViewGroup)
              LayoutInflater.from(getContext())
                  .inflate(R.layout.simple_spinner_dropdown_item, parent, false);
      ((TextView) vg.findViewById(R.id.text1)).setText(getItem(position).toString());
      v = vg;
    }

    parent.setVerticalScrollBarEnabled(false);
    return v;
  }
}
