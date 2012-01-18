package com.todoroo.astrid.ui;

import android.app.Activity;
import android.widget.ImageView;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.astrid.ui.ImportanceControlSet.ImportanceChangedListener;

public class EditTitleControlSet extends EditTextControlSet implements ImportanceChangedListener {

    ImageView importance;

    private static int[] IMPORTANCE_DRAWABLES = { R.drawable.importance_title_1, R.drawable.importance_title_2, R.drawable.importance_title_3,
        R.drawable.importance_title_4, R.drawable.importance_title_5, R.drawable.importance_title_6 };

    public EditTitleControlSet(Activity activity, int layout, StringProperty property, int editText) {
        super(activity, layout, property, editText);
        importance = (ImageView) getView().findViewById(R.id.importance);
    }

    @Override
    public void importanceChanged(int i, int color) {
        if(importance != null)
            importance.setImageResource(IMPORTANCE_DRAWABLES[i]);
    }

}
