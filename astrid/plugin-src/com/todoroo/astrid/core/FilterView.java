/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Draws filters
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class FilterView extends View {

    private int start = 0, end = 0, max = 1;

    private static final int FILTER_COLOR = Color.rgb(0x1f, 0x78, 0xb4);
    private static final int BG_COLOR = Color.rgb(0xe9, 0xe9, 0xe9);
    private static final int TEXT_COLOR = Color.WHITE;

    // --- boilerplate

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public FilterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public FilterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FilterView(Context context) {
        super(context);
    }

    // --- painting code

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(BG_COLOR);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        paint.setColor(FILTER_COLOR);
        Path path = new Path();
        path.moveTo(getWidth() * (0.5f - 0.5f * start / max), 0);
        path.lineTo(getWidth() * (0.5f + 0.5f * start / max), 0);
        path.lineTo(getWidth() * (0.5f + 0.5f * end / max), getHeight());
        path.lineTo(getWidth() * (0.5f - 0.5f * end / max), getHeight());
        path.close();
        canvas.drawPath(path, paint);

        paint.setColor(TEXT_COLOR);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(16);
        canvas.drawText(Integer.toString(end), getWidth() / 2, getHeight() / 2 + 8, paint);
    }

}
