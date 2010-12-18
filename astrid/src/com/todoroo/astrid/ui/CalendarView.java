package com.todoroo.astrid.ui;

import java.util.Calendar;
import java.util.Date;

import android.content.Context;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.timsu.astrid.R;

public class CalendarView extends View {

	private static final int PADDING = 3;
	private final static int CURVE_RADIUS = 5;
	private final static int TEXT_SIZE = 16;
    private static final float MONTH_TEXT_SIZE = 22;

	private Paint borderPaint;
	private Paint borderRightAlignPaint;
	private Paint backColorPaint;
	private Paint whiteCenterAlignLargePaint;
	private Paint darkRightAlignPaint;
	private Paint todayCalendarPaint;
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

	private int boxWidth;
	private int boxHeight;

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

    	borderPaint = new Paint();
    	borderPaint.setAntiAlias(true);
    	borderPaint.setColor(Color.BLACK);

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
    	calendarPaint.setColor(Color.rgb(202, 201, 194));

    	whiteCenterAlignLargePaint = new Paint();
    	whiteCenterAlignLargePaint.setAntiAlias(true);
    	whiteCenterAlignLargePaint.setColor(Color.WHITE);
    	whiteCenterAlignLargePaint.setTextAlign(Paint.Align.CENTER);
    	whiteCenterAlignLargePaint.setTextSize(MONTH_TEXT_SIZE * density);

    	darkRightAlignPaint = new Paint();
    	darkRightAlignPaint.setAntiAlias(true);
    	darkRightAlignPaint.setColor(Color.rgb(40, 40, 40));
    	darkRightAlignPaint.setTextAlign(Paint.Align.RIGHT);
    	darkRightAlignPaint.setTextSize(TEXT_SIZE * density);

    	todayCalendarPaint = new Paint();
    	todayCalendarPaint.setAntiAlias(true);
    	todayCalendarPaint.setColor(Color.rgb(222, 221, 154));

    	backColorPaint = new Paint();
    	backColorPaint.setAntiAlias(true);
    	backColorPaint.setColor(Color.rgb(68, 68, 68));

        setPadding(PADDING, PADDING, PADDING, PADDING);
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
        RectF outerMostBorder = new RectF();

        outerMostBorder.set(5, 5, getMeasuredWidth() - 5, getMeasuredHeight() - 5);
        canvas.drawRoundRect(outerMostBorder, CURVE_RADIUS, CURVE_RADIUS, borderPaint);

        outerMostBorder.set(6, 6, getMeasuredWidth() - 6, getMeasuredHeight() - 6);
        canvas.drawRoundRect(outerMostBorder, CURVE_RADIUS, CURVE_RADIUS, backColorPaint);
        // Outermost border -- end

        // Month border -- Start
        RectF rectF = new RectF();

        float monthTitleHeight = (MONTH_TEXT_SIZE + 30) * density;
        rectF.set(15, 15, getMeasuredWidth() - 15, monthTitleHeight);
        canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, borderPaint);

        rectF.set(16, 16, getMeasuredWidth() - 16, monthTitleHeight - 1);
        // canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, backColorPaint);
        // Month border -- end

        // Month left arrow -- Start
        Bitmap leftArrow = ((BitmapDrawable)getResources().getDrawable(R.drawable.cal_left)).getBitmap();
        leftArrowHeight = (int)(leftArrow.getHeight()*density);
        leftArrowWidth = (int)(leftArrow.getWidth()*density);
        leftArrowX = 24;
        leftArrowY = 8 + (int)((monthTitleHeight / 2 - leftArrowHeight/2));
        canvas.drawBitmap(leftArrow, new Rect(0,0,leftArrow.getWidth(),leftArrow.getHeight()),
                new Rect(leftArrowX, leftArrowY, leftArrowX + leftArrowWidth,
                        leftArrowY + leftArrowHeight), null);
        // Month left arrow -- End

        // Month right arrow -- Start
        Bitmap rightArrow = ((BitmapDrawable)getResources().getDrawable(R.drawable.cal_right)).getBitmap();
        rightArrowHeight = (int)(rightArrow.getHeight()*density);
        rightArrowWidth = (int)(rightArrow.getWidth()*density);
        rightArrowX = (int) (getMeasuredWidth() - (16 * density) - (PADDING*3) - rightArrow.getWidth());
        rightArrowY = 8 + (int)((monthTitleHeight / 2 - rightArrowHeight/2));
        canvas.drawBitmap(rightArrow, new Rect(0,0,rightArrow.getWidth(),rightArrow.getHeight()),
                new Rect(rightArrowX, rightArrowY, rightArrowX + rightArrowWidth,
                        rightArrowY + rightArrowHeight), null);
        // Month right arrow -- End

        // Month text -- Start
        int monthX = getMeasuredWidth() / 2;
        int monthY = (int) (monthTitleHeight / 2 + 15);
        String monthYear = (String) DateFormat.format("MMMM yyyy", calendarDate); //$NON-NLS-1$
        canvas.drawText(monthYear, monthX, monthY, whiteCenterAlignLargePaint);
        // Month text -- End

        // Day heading -- Start
        int dayLeft = 15;
        int dayTop = (int)(monthTitleHeight + PADDING * 2);

        boxWidth = (getMeasuredWidth() - 38 - (PADDING*2)) / 7;
        boxHeight = (int) (((getMeasuredHeight() - (monthTitleHeight) - 16) - (PADDING * 8)) / 7);

        int textX = 0;
        int textY = 0;

        Calendar calendar = Calendar.getInstance();
        int firstDayOfWeek = calendar.getFirstDayOfWeek();
        calendar.set(Calendar.DAY_OF_WEEK, firstDayOfWeek);
        for(int i = 0; i < 7; i++) {
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            String day = DateUtils.getDayOfWeekString(dayOfWeek, DateUtils.LENGTH_SHORT);
            calendar.add(Calendar.DATE, 1);

        	rectF.set(dayLeft, dayTop, dayLeft + boxWidth, dayTop + boxHeight);
            canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, borderPaint);

            rectF.set(dayLeft+1, dayTop+1, dayLeft + boxWidth - 1, dayTop + boxHeight - 1);
            canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, dayPaint);

            textX = dayLeft + boxWidth - PADDING * 2;
            textY = dayTop + (boxHeight - boxHeight/8) - PADDING * 2;
            canvas.drawText(day, textX, textY, darkRightAlignPaint);

            dayLeft += boxWidth + PADDING;
        }
        // Day heading -- End

        // Calendar -- Start
        calendar.setTime(calendarDate);

        if (currentHighlightDay == -1) {
        	currentHighlightDay = calendar.get(Calendar.DATE);
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
        Paint colorPaint, textPaint;

        dayLeftArr = new int[lastDateOfThisMonth];
        dayTopArr = new int[lastDateOfThisMonth];
        for (int i = 1; i <= 6; i++) {
        	dayLeft = 15;
	        dayTop += boxHeight + PADDING;
			for (int j = 1; j <= 7; j++) {
				if (firstTime && j != firstDayOfMonth) {
					dayLeft += boxWidth + PADDING;
					continue;
				}

				firstTime = false;

				if (dayOfMonth <= lastDateOfThisMonth) {
					if (currentHighlightDay == dayOfMonth) {
						colorPaint = darkRightAlignPaint;
						textPaint = borderRightAlignPaint;
					} else if(today == dayOfMonth) {
					    colorPaint = todayCalendarPaint;
					    textPaint = darkRightAlignPaint;
					} else {
						colorPaint = calendarPaint;
						textPaint = darkRightAlignPaint;
					}
					dayLeftArr[dayOfMonth-1] = dayLeft;
					dayTopArr[dayOfMonth-1] = dayTop;
					rectF.set(dayLeft, dayTop, dayLeft + boxWidth, dayTop + boxHeight);
		            canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, borderPaint);

		            rectF.set(dayLeft+1, dayTop+1, dayLeft + boxWidth - 1, dayTop + boxHeight - 1);
		            canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, colorPaint);

		            textX = dayLeft + boxWidth - PADDING * 2;
		            textY = dayTop + (boxHeight - boxHeight/8) - PADDING * 2;
		            canvas.drawText(String.valueOf(dayOfMonth), textX, textY, textPaint);

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
                && (y > 5 && y < (leftArrowY * 2))) {
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
        } else if ((x > rightArrowX - rightArrowWidth && x < (rightArrowX + rightArrowWidth))
                && (y > 5 && y < (rightArrowY * 2))) {
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
            // Handle left-right arrow click -- end
		} else {
			// Check if clicked on date
			for (int i=0; i<dayLeftArr.length; i++) {
				if ((x > dayLeftArr[i] && x < dayLeftArr[i]+boxWidth) && (y > dayTopArr[i] && y < dayTopArr[i] + boxHeight)) {
					currentHighlightDay = i+1;
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(calendarDate);
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
	}
}
