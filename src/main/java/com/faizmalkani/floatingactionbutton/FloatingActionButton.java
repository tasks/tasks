package com.faizmalkani.floatingactionbutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.tasks.R;

import static com.todoroo.andlib.utility.AndroidUtilities.atLeastHoneycomb;

public class FloatingActionButton extends View {

    private final Paint mButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG) {{
        setStyle(Style.FILL);
    }};
    private final Paint mButtonPaintStroke = new Paint(Paint.ANTI_ALIAS_FLAG) {{
        setStyle(Style.STROKE);
    }};
    private final Paint mDrawablePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap mBitmap;
    private int tint;
    private int stroke;

    public FloatingActionButton(Context context) {
        this(context, null);
    }

    public FloatingActionButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.FloatingActionButton);
        tint = a.getColor(R.styleable.FloatingActionButton_tint, Color.WHITE);
        stroke = a.getColor(R.styleable.FloatingActionButton_stroke, Color.TRANSPARENT);
        mButtonPaint.setColor(tint);
        mButtonPaintStroke.setColor(stroke);
        mButtonPaint.setShadowLayer(
                a.getFloat(R.styleable.FloatingActionButton_shadowRadius, 10.0f),
                a.getFloat(R.styleable.FloatingActionButton_shadowDx, 0.0f),
                a.getFloat(R.styleable.FloatingActionButton_shadowDy, 3.5f),
                a.getInteger(R.styleable.FloatingActionButton_shadowColor, Color.argb(100, 0, 0, 0)));

        Drawable drawable = a.getDrawable(R.styleable.FloatingActionButton_drawable);
        if (null != drawable) {
            mBitmap = ((BitmapDrawable) drawable).getBitmap();
        }
        setWillNotDraw(false);
        if (atLeastHoneycomb()) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    private static int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, (float) (getWidth() / 2.6), mButtonPaint);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, (float) (getWidth() / 2.6), mButtonPaintStroke);
        if (null != mBitmap) {
            canvas.drawBitmap(mBitmap, (getWidth() - mBitmap.getWidth()) / 2,
                    (getHeight() - mBitmap.getHeight()) / 2, mDrawablePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int color;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            color = tint;
        } else {
            color = darkenColor(tint);
        }
        mButtonPaint.setColor(color);
        invalidate();
        return super.onTouchEvent(event);
    }
}