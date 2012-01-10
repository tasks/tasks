package com.todoroo.astrid.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class TextViewWithMeasureListener extends TextView {

    public interface OnTextMeasureListener {
        public void onTextSizeChanged();
    }

    private OnTextMeasureListener listener;


    public TextViewWithMeasureListener(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (listener != null) {
            listener.onTextSizeChanged();
        }
    }

    public OnTextMeasureListener getOnTextSizeChangedListener() {
        return listener;
    }

    public void setOnTextSizeChangedListener(OnTextMeasureListener listener) {
        this.listener = listener;
    }
}
