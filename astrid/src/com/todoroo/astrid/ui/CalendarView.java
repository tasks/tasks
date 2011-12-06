package com.todoroo.astrid.ui;

import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.timsu.astrid.R;

public class CalendarView extends View {

	private static final int PADDING = 0;
	private static final int TEXT_PADDING = 2;
	private final static int OUTER_BORDER_RADIUS = 5;
	private final static int CURVE_RADIUS = 0;
	private final static int TEXT_SIZE = 16;
    private static final float MONTH_TEXT_SIZE = 22;
    private static final float GESTURE_DELTAX_THRESHOLD = 100;
    private float deltaX;
    private boolean ignoreNextTouch;

	private Paint borderPaint;
	private Paint borderRightAlignPaint;
	private Paint backColorPaint;
	private Paint whiteCenterAlignLargePaint;
	private Paint centerAlignPaint;
	private Paint todayCalendarPaint;
	private Paint selectedCalendarPaint;
	private Paint dayPaint;
	private Paint calendarPaint;
	private float density;

	private int leftArrowHeight;
	private int leftArrowWidth;
	private int rightArrowHeight;
	private int rightArrowWidth;
	private int leftArrowX = 0;
	private int leftArrowY = 0;
	private int rightArrowX = 0;
	private int rightArrowY = 0;

	private int[] dayLeftArr;
	private int[] dayTopArr;

	private float boxWidth;
	private float boxHeight;

	private Date calendarDate = new Date();
	private int currentHighlightDay = -1;

	public interface OnSelectedDateListener {
	    public void onSelectedDate(Date date);
	}
	private OnSelectedDateListener onSelectedDateListener;

	public void setOnSelectedDateListener(
            OnSelectedDateListener onSelectedDateListener) {
        this.onSelectedDateListener = onSelectedDateListener;
    }

    /**
     * Constructor.  This version is only needed if you will be instantiating
     * the object manually (not from a layout XML file).
     * @param context
     */
    public CalendarView(Context context) {
        super(context);
        initCalendarView(context);
    }

    public CalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCalendarView(context);
    }

    private final void initCalendarView(Context context) {
        Display display = ((WindowManager) context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();

        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        density = metrics.density;

        Resources r = context.getResources();

    	borderPaint = new Paint();
    	borderPaint.setAntiAlias(true);
    	borderPaint.setColor(Color.WHITE);

    	borderRightAlignPaint = new Paint();
    	borderRightAlignPaint.setAntiAlias(true);
    	borderRightAlignPaint.setColor(Color.WHITE);
    	borderRightAlignPaint.setTextSize(TEXT_SIZE * density);
    	borderRightAlignPaint.setTextAlign(Paint.Align.RIGHT);

    	dayPaint = new Paint();
    	dayPaint.setAntiAlias(true);
    	dayPaint.setColor(Color.rgb(137, 135, 132));

    	calendarPaint = new Paint();
    	calendarPaint.setAntiAlias(true);
    	calendarPaint.setColor(Color.BLACK);

    	whiteCenterAlignLargePaint = new Paint();
    	whiteCenterAlignLargePaint.setAntiAlias(true);
    	whiteCenterAlignLargePaint.setColor(Color.WHITE);
    	whiteCenterAlignLargePaint.setTextAlign(Paint.Align.CENTER);
    	whiteCenterAlignLargePaint.setTextSize(MONTH_TEXT_SIZE * density);

    	centerAlignPaint = new Paint();
    	centerAlignPaint.setAntiAlias(true);
    	centerAlignPaint.setColor(Color.WHITE);
    	centerAlignPaint.setTextAlign(Paint.Align.CENTER);
    	centerAlignPaint.setTextSize(TEXT_SIZE * density);

    	todayCalendarPaint = new Paint();
    	todayCalendarPaint.setAntiAlias(true);
    	todayCalendarPaint.setColor(r.getColor(android.R.color.darker_gray));

    	selectedCalendarPaint = new Paint();
    	selectedCalendarPaint.setAntiAlias(true);
    	selectedCalendarPaint.setColor(r.getColor(R.color.task_edit_date_shortcuts_bg));

    	backColorPaint = new Paint();
    	backColorPaint.setAntiAlias(true);
    	backColorPaint.setColor(Color.BLACK);

        setPadding(PADDING, PADDING, PADDING, PADDING);

        final GestureDetector swipeCalendarListener = new GestureDetector(new SimpleOnGestureListener() {

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (distanceX < 0 && deltaX > 0 || distanceX > 0 && deltaX < 0) { // Reset if direction changed
                    deltaX = 0;
                }

                if (Math.abs(deltaX) >  GESTURE_DELTAX_THRESHOLD) {
                    if (deltaX > 0) {
                        performClick(rightArrowX + rightArrowWidth / 2, rightArrowY + rightArrowHeight / 2);
                    } else {
                        performClick(leftArrowX + leftArrowWidth / 2, leftArrowY + leftArrowHeight / 2);
                    }
                    ignoreNextTouch = true;
                    deltaX = 0;
                } else {
                    deltaX += distanceX;
                }
                return true;
            }
        });

        final OnTouchListener swipeTouchListener = new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return swipeCalendarListener.onTouchEvent(event);
            }
        };

        this.setOnTouchListener(swipeTouchListener);
    }

    /**
     * @see android.view.View#measure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec));
    }

    /**
     * Determines the width of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        }

        return result;
    }

    /**
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        }
        return result;
    }

    /**
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Background
        canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), backColorPaint);

        // Outermost border -- Start
//        RectF outerMostBorder = new RectF();
//
//        outerMostBorder.set(5, 5, getMeasuredWidth() - 5, getMeasuredHeight() - 5);
//        canvas.drawRoundRect(outerMostBorder, OUTER_BORDER_RADIUS, OUTER_BORDER_RADIUS, borderPaint);
//
//        outerMostBorder.set(6, 6, getMeasuredWidth() - 6, getMeasuredHeight() - 6);
//        canvas.drawRoundRect(outerMostBorder, CURVE_RADIUS, CURVE_RADIUS, backColorPaint);
        // Outermost border -- end

        // Month border -- Start
        RectF rectF = new RectF();

        float monthTitleHeight = (MONTH_TEXT_SIZE + 30) * density;
        rectF.set(15, 15, getMeasuredWidth() - 15, monthTitleHeight);
        canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, backColorPaint);

        rectF.set(16, 16, getMeasuredWidth() - 16, monthTitleHeight - 1);
        // canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, backColorPaint);
        // Month border -- end

        // Month left arrow -- Start
        Bitmap leftArrow = ((BitmapDrawable)getResources().getDrawable(R.drawable.icn_arrow_left)).getBitmap();
        leftArrowHeight = (int)(leftArrow.getHeight()*density / 2);
        leftArrowWidth = (int)(leftArrow.getWidth()*density / 2);
        leftArrowX = 5;
        leftArrowY = 8 + (int)((monthTitleHeight / 2 - leftArrowHeight/2));
        canvas.drawBitmap(leftArrow, new Rect(0,0,leftArrow.getWidth(),leftArrow.getHeight()),
                new Rect(leftArrowX, leftArrowY, leftArrowX + leftArrowWidth,
                        leftArrowY + leftArrowHeight), null);
        // Month left arrow -- End

        // Month right arrow -- Start
        Bitmap rightArrow = ((BitmapDrawable)getResources().getDrawable(R.drawable.icn_arrow_right)).getBitmap();
        rightArrowHeight = (int)(rightArrow.getHeight()*density / 2);
        rightArrowWidth = (int)(rightArrow.getWidth()*density / 2);
        rightArrowX = (int) (getMeasuredWidth() - (2 * density) - (PADDING*3) - rightArrow.getWidth());
        rightArrowY = 8 + (int)((monthTitleHeight / 2 - rightArrowHeight/2));
        canvas.drawBitmap(rightArrow, new Rect(0,0,rightArrow.getWidth(),rightArrow.getHeight()),
                new Rect(rightArrowX, rightArrowY, rightArrowX + rightArrowWidth,
                        rightArrowY + rightArrowHeight), null);
        // Month right arrow -- End

        Calendar calendar = Calendar.getInstance();

        // Month text -- Start
        int monthX = getMeasuredWidth() / 2;
        int monthY = (int) (monthTitleHeight / 2 + 15);
        String monthYear = (String) DateFormat.format("MMMM yyyy", calendarDate.getTime() == 0 ? calendar.getTime() : calendarDate); //$NON-NLS-1$
        canvas.drawText(monthYear, monthX, monthY, whiteCenterAlignLargePaint);
        // Month text -- End

        // Day heading -- Start
        int dayLeft = 3;
        int dayTop = (int)(monthTitleHeight + PADDING * 2);

        boxWidth = (getMeasuredWidth() - (PADDING*2)) / 7.0f;
        boxHeight = (int) (((getMeasuredHeight() - (monthTitleHeight) - 16) - (PADDING * 8)) / 7);

        float textX = 0;
        float textY = 0;

        int firstDayOfWeek = calendar.getFirstDayOfWeek();
        calendar.set(Calendar.DAY_OF_WEEK, firstDayOfWeek);
        for(int i = 0; i < 7; i++) {
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            String day = DateUtils.getDayOfWeekString(dayOfWeek, DateUtils.LENGTH_SHORT);
            calendar.add(Calendar.DATE, 1);

            textX = dayLeft + boxWidth / 2;
            textY = dayTop + (boxHeight - boxHeight/8) - TEXT_PADDING * 2;
            canvas.drawText(day, textX, textY, centerAlignPaint);

            dayLeft += boxWidth;
        }
        // Day heading -- End

        // Calendar -- Start
        calendar.setTime(calendarDate.getTime() == 0 ? calendar.getTime() : calendarDate);

        if (currentHighlightDay == -1) {
        	currentHighlightDay = calendarDate.getTime() == 0 ? 0 : calendar.get(Calendar.DATE);
        }
        int today = -1;
        Calendar todayCalendar = Calendar.getInstance();
        if(calendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                calendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR))
            today = todayCalendar.get(Calendar.DATE);
        int lastDateOfThisMonth = calendar.getActualMaximum(Calendar.DATE);

        calendar.set(Calendar.DATE, 1);
        // offset for day of week
        int firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + Calendar.SUNDAY;
        if(firstDayOfMonth == 0)
            firstDayOfMonth = 7;
        boolean firstTime = true;
        int dayOfMonth = 1;
        Paint colorPaint;

        dayLeftArr = new int[lastDateOfThisMonth];
        dayTopArr = new int[lastDateOfThisMonth];
        for (int i = 1; i <= 6; i++) {
        	dayLeft = 1;
	        dayTop += boxHeight + PADDING;
			for (int j = 1; j <= 7; j++) {
				if (firstTime && j != firstDayOfMonth) {
					dayLeft += boxWidth + PADDING;
					continue;
				}

				firstTime = false;

				if (dayOfMonth <= lastDateOfThisMonth) {
					if (currentHighlightDay == dayOfMonth) {
						colorPaint = selectedCalendarPaint;
					} else if(today == dayOfMonth) {
					    colorPaint = todayCalendarPaint;
					} else {
						colorPaint = calendarPaint;
					}
					dayLeftArr[dayOfMonth-1] = dayLeft;
					dayTopArr[dayOfMonth-1] = dayTop;
					rectF.set(dayLeft, dayTop, dayLeft + boxWidth, dayTop + boxHeight);
		            canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, borderPaint);

		            rectF.set(dayLeft+1, dayTop+1, dayLeft + boxWidth - 1, dayTop + boxHeight - 1);
		            canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, colorPaint);

		            textX = dayLeft + boxWidth - TEXT_PADDING * 3;
		            textY = dayTop + borderRightAlignPaint.getTextSize() + TEXT_PADDING;
		            canvas.drawText(String.valueOf(dayOfMonth), textX, textY, borderRightAlignPaint);

		            dayLeft += boxWidth + PADDING;

		            dayOfMonth++;
				}
			}
		}
        // Calendar -- End
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	switch (event.getAction()) {
    	case MotionEvent.ACTION_UP:
    		int x = (int) event.getX();
            int y = (int) event.getY();
            performClick(x, y);
            break;
    	}
        return true;
    }

    private void performClick(int x, int y) {
    	// System.out.println("---------------------Current x, y : " + x + ", " + y);
    	// Handle left-right arrow click -- start
        if ((x > leftArrowX && x < (leftArrowX + leftArrowWidth * 2))
                && (y > leftArrowY - leftArrowHeight / 2 && y < (leftArrowY + 3 * leftArrowHeight / 2))) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(calendarDate);
            int currentDay = calendar.get(Calendar.DATE);
            calendar.set(Calendar.DATE, 1);
            calendar.add(Calendar.MONTH, -1);
            int lastDay = calendar.getActualMaximum(Calendar.DATE);
            calendar.set(Calendar.DATE, Math.min(currentDay, lastDay));
            calendarDate = calendar.getTime();
            currentHighlightDay = calendar.get(Calendar.DATE);
            this.invalidate();

            if(onSelectedDateListener != null)
                onSelectedDateListener.onSelectedDate(calendarDate);
        } else if ((x > rightArrowX - rightArrowWidth && x < (rightArrowX + rightArrowWidth))
                && (y > rightArrowY - rightArrowHeight / 2 && y < (rightArrowY + 3 * rightArrowHeight / 2))) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(calendarDate);
            int currentDay = calendar.get(Calendar.DATE);
            calendar.set(Calendar.DATE, 1);
            calendar.add(Calendar.MONTH, 1);
            int lastDay = calendar.getActualMaximum(Calendar.DATE);
            calendar.set(Calendar.DATE, Math.min(currentDay, lastDay));
            calendarDate = calendar.getTime();
            currentHighlightDay = calendar.get(Calendar.DATE);
            this.invalidate();

            if(onSelectedDateListener != null)
                onSelectedDateListener.onSelectedDate(calendarDate);
            // Handle left-right arrow click -- end
		} else if(dayLeftArr != null) {
			// Check if clicked on date
		    if (ignoreNextTouch) {
		        ignoreNextTouch = false;
		        return;
		    }
			for (int i=0; i<dayLeftArr.length; i++) {
				if ((x > dayLeftArr[i] && x < dayLeftArr[i]+boxWidth) && (y > dayTopArr[i] && y < dayTopArr[i] + boxHeight)) {
					currentHighlightDay = i+1;
					Calendar calendar = Calendar.getInstance();
					Date today = calendar.getTime();
					today.setTime(today.getTime() / 1000L * 1000L);
					today.setHours(23);
					today.setMinutes(59);
					today.setSeconds(59);
					calendar.setTime(calendarDate.getTime() == 0 ? today : calendarDate);
					calendar.set(Calendar.DATE, currentHighlightDay);

					calendarDate = calendar.getTime();
		            this.invalidate();

		            if(onSelectedDateListener != null)
		                onSelectedDateListener.onSelectedDate(calendarDate);
				}
			}
		}
    }

    public Date getCalendarDate() {
    	return calendarDate;
    }

	public void setCalendarDate(Date calendarDate) {
		this.calendarDate = calendarDate;
		currentHighlightDay = -1;
	}
}
