package com.todoroo.astrid.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

public class EditDialogOkBackground {

    public static StateListDrawable getBg(int colorValue) {
        final Paint p = new Paint();
        p.setColor(Color.GRAY);
        Drawable d = new Drawable() {
            @Override
            public void setColorFilter(ColorFilter cf) {
                //
            }

            @Override
            public void setAlpha(int alpha) {
                //
            }

            @Override
            public int getOpacity() {
                return PixelFormat.OPAQUE;
            }

            @Override
            public void draw(Canvas canvas) {
                Rect r = canvas.getClipBounds();
                canvas.drawLine(r.left, r.top, r.right, r.top, p);
            }
        };

        ColorDrawable color = new ColorDrawable(colorValue);

        StateListDrawable stld = new StateListDrawable();
        stld.addState(new int[] { android.R.attr.state_pressed }, color);
        stld.addState(new int[] { android.R.attr.state_enabled }, d);
        return stld;
    }

}
