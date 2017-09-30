package org.tasks.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import org.tasks.R;
import org.tasks.themes.ThemeAccent;

import java.util.List;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastLollipop;

public class SingleCheckedArrayAdapter extends ArrayAdapter<String> {

    @NonNull private final Context context;
    private final List<String> items;
    private final ThemeAccent accent;
    private int checkedPosition = -1;

    public SingleCheckedArrayAdapter(@NonNull Context context, @NonNull List<String> items, ThemeAccent accent) {
        this(context, R.layout.simple_list_item_single_choice_themed, items, accent);
    }

    public SingleCheckedArrayAdapter(@NonNull Context context, int layout, @NonNull List<String> items, ThemeAccent accent) {
        super(context, layout, items);
        this.context = context;
        this.items = items;
        this.accent = accent;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
        if (this.checkedPosition == position) {
            if (atLeastLollipop()) {
                view.setCheckMarkDrawable(R.drawable.ic_check_white_24dp);
            } else {
                Drawable original = ContextCompat.getDrawable(context, R.drawable.ic_check_white_24dp);
                Drawable wrapped = DrawableCompat.wrap(original.mutate());
                DrawableCompat.setTint(wrapped, accent.getAccentColor());
                view.setCheckMarkDrawable(wrapped);
            }
            view.setChecked(true);
        } else {
            view.setCheckMarkDrawable(null);
            view.setChecked(false);
        }
        return view;
    }

    public void setChecked(String item) {
        setChecked(items.indexOf(item));
    }

    public void setChecked(int position) {
        this.checkedPosition = position;
    }
}
