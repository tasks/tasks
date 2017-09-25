package org.tasks.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import org.tasks.R;

import java.util.List;

public class SingleCheckedArrayAdapter<T> extends ArrayAdapter<T> {

    private final List<T> items;
    private int checkedPosition = -1;

    public SingleCheckedArrayAdapter(@NonNull Context context, @NonNull List<T> items) {
        super(context, R.layout.simple_list_item_single_choice_themed, items);
        this.items = items;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
        if (this.checkedPosition == position) {
            view.setCheckMarkDrawable(R.drawable.ic_check_white_24dp);
            view.setChecked(true);
        } else {
            view.setCheckMarkDrawable(null);
            view.setChecked(false);
        }
        return view;
    }

    public void setChecked(T item) {
        setChecked(items.indexOf(item));
    }

    public void setChecked(int position) {
        this.checkedPosition = position;
    }
}
