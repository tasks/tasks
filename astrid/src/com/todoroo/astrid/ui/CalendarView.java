package com.todoroo.astrid.ui;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.timsu.astrid.R;

public class CalendarView extends View {

	private static final int PADDING = 3;
	private final static int CURVE_RADIUS = 5;
	private final static int TEXT_SIZE = 16;
	private final static String[] DAYS = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};

	private Paint borderPaint;
	private Paint borderRightAlignPaint;
	private Paint backColorPaint;
	private Paint whiteCenterAlignPaint;
	private Paint whiteRightAlignPaint;
	private Paint dayPaint;
	private Paint calendarPaint;

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

    /**
     * Constructor.  This version is only needed if you will be instantiating
     * the object manually (not from a layout XML file).
     * @param context
     */
    public CalendarView(Context context) {
        super(context);
        initCalendarView();
    }

    public CalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCalendarView();
    }

    private final void initCalendarView() {
    	borderPaint = new Paint();
    	borderPaint.setAntiAlias(true);
    	borderPaint.setColor(Color.BLACK);

    	borderRightAlignPaint = new Paint();
    	borderRightAlignPaint.setAntiAlias(true);
    	borderRightAlignPaint.setColor(Color.BLACK);
    	borderRightAlignPaint.setTextSize(TEXT_SIZE);
    	borderRightAlignPaint.setTextAlign(Paint.Align.RIGHT);

    	dayPaint = new Paint();
    	dayPaint.setAntiAlias(true);
    	dayPaint.setColor(Color.rgb(137, 135, 132));

    	calendarPaint = new Paint();
    	calendarPaint.setAntiAlias(true);
    	calendarPaint.setColor(Color.rgb(202, 201, 194));

    	whiteCenterAlignPaint = new Paint();
    	whiteCenterAlignPaint.setAntiAlias(true);
    	whiteCenterAlignPaint.setColor(Color.WHITE);
    	whiteCenterAlignPaint.setTextAlign(Paint.Align.CENTER);
    	whiteCenterAlignPaint.setTextSize(TEXT_SIZE);

    	whiteRightAlignPaint = new Paint();
    	whiteRightAlignPaint.setAntiAlias(true);
    	whiteRightAlignPaint.setColor(Color.WHITE);
    	whiteRightAlignPaint.setTextAlign(Paint.Align.RIGHT);
    	whiteRightAlignPaint.setTextSize(TEXT_SIZE);

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

        rectF.set(15, 15, getMeasuredWidth() - 15, (int)(getMeasuredHeight() * 0.2)); // 20% height is given to month border
        canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, borderPaint);

        rectF.set(16, 16, getMeasuredWidth() - 16, (int)(getMeasuredHeight() * 0.2) - 1);
        canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, backColorPaint);
        // Month border -- end

        // Month left arrow -- Start
        InputStream is = getResources().openRawResource(R.drawable.leftarrow);
        Bitmap leftArrow = BitmapFactory.decodeStream(is);

        leftArrowHeight = leftArrow.getHeight();
        leftArrowWidth = leftArrow.getWidth();
        leftArrowX = 24;
        leftArrowY = 8 + (int)((getMeasuredHeight() * 0.2 / 2) - (leftArrowHeight/2));
        System.out.println("Left arrow x, y, height, width : " + leftArrowX + ", " + leftArrowY + ", " + leftArrowHeight + ", " + leftArrowWidth);
        canvas.drawBitmap(leftArrow, leftArrowX, leftArrowY, null);
        // Month left arrow -- End

        // Month right arrow -- Start
        is = getResources().openRawResource(R.drawable.rightarrow);
        Bitmap rightArrow = BitmapFactory.decodeStream(is);
        rightArrowHeight = rightArrow.getHeight();
        rightArrowWidth = rightArrow.getWidth();
        rightArrowX = getMeasuredWidth() - 16 - (PADDING*3) - rightArrow.getWidth();
        rightArrowY = 8 + (int)((getMeasuredHeight() * 0.2 / 2) - (rightArrowHeight/2));
        // System.out.println("Right arrow x, y, height, width : " + rightArrowX + ", " + rightArrowY + ", " + rightArrowHeight + ", " + rightArrowWidth);
        canvas.drawBitmap(rightArrow, rightArrowX, rightArrowY, null);
        // Month right arrow -- End

        // Month text -- Start
        int monthX = getMeasuredWidth() / 2;
        int monthY = (int) (getMeasuredHeight() * 0.2 / 2) + 14;
        String monthYear = (String) DateFormat.format("MMMM yyyy", calendarDate);
        canvas.drawText(monthYear, monthX, monthY, whiteCenterAlignPaint);
        // Month text -- End

        // Day heading -- Start
        int dayLeft = 15;
        int dayTop = (int)(getMeasuredHeight() * 0.2) + (PADDING * 2);

        boxWidth = (getMeasuredWidth() - 38 - (PADDING*2)) / 7;
        boxHeight = (int) (((getMeasuredHeight() - (getMeasuredHeight() * 0.2) - 16) - (PADDING * 8)) / 7);
//        int boxHeight = boxWidth;

        int textX = 0;
        int textY = 0;
        for (String day : DAYS) {
        	rectF.set(dayLeft, dayTop, dayLeft + boxWidth, dayTop + boxHeight);
            canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, borderPaint);

            rectF.set(dayLeft+1, dayTop+1, dayLeft + boxWidth - 1, dayTop + boxHeight - 1);
            canvas.drawRoundRect(rectF, CURVE_RADIUS, CURVE_RADIUS, dayPaint);

            String strDateFormat = "E";
            SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);

            textX = dayLeft + boxWidth - PADDING * 2;
            textY = dayTop + (boxHeight - boxHeight/8) - PADDING * 2;
            canvas.drawText(day, textX, textY, whiteRightAlignPaint);

            dayLeft += boxWidth + PADDING;
        }
        // Day heading -- End

        // Calendar -- Start
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(calendarDate);

        if (currentHighlightDay == -1) {
        	currentHighlightDay = calendar.get(Calendar.DATE);
        }
        int lastDateOfThisMonth = calendar.getActualMaximum(Calendar.DATE);

        calendar.set(Calendar.DATE, 1);
        // SUNDAY is 1 in java.util.Calendar
        int firstDay = calendar.get(Calendar.DAY_OF_WEEK);
        // Setting as per our calendar, as it starts from monday and not sunday
        if (firstDay == Calendar.SUNDAY) {
        	firstDay = 7;
        } else {
        	firstDay--;
        }
        boolean firstTime = true;
        int dayOfMonth = 1;
        Paint colorPaint, textPaint;

        dayLeftArr = new int[lastDateOfThisMonth];
        dayTopArr = new int[lastDateOfThisMonth];
        for (int i = 1; i <= 6; i++) {
        	dayLeft = 15;
	        dayTop += boxHeight + PADDING;
			for (int j = 1; j <= 7; j++) {
				if (firstTime && j != firstDay) {
					dayLeft += boxWidth + PADDING;
					continue;
				}

				firstTime = false;

				if (dayOfMonth <= lastDateOfThisMonth) {
					if (currentHighlightDay == dayOfMonth) {
						colorPaint = whiteRightAlignPaint;
						textPaint = borderRightAlignPaint;
					} else {
						colorPaint = calendarPaint;
						textPaint = whiteRightAlignPaint;
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
		if ((x > leftArrowX && x < (leftArrowX + leftArrowWidth))
				&& (y > leftArrowY && y < (leftArrowY + leftArrowHeight))) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(calendarDate);
			int prevMonth = calendar.get(Calendar.MONTH);

			int currentMonth = calendar.get(Calendar.MONTH);
			if (currentMonth == Calendar.JANUARY) {
				calendar.set(Calendar.MONTH, Calendar.DECEMBER);
				calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
			} else {
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
			}
			calendarDate = calendar.getTime();
			calendar.setTime(calendarDate);
			if (prevMonth == calendar.get(Calendar.MONTH)) {
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
				calendar.set(Calendar.DATE, 1);
				calendarDate = calendar.getTime();
			}
			currentHighlightDay = calendar.get(Calendar.DATE);
			this.invalidate();
		} else if ((x > rightArrowX && x < (rightArrowX + rightArrowWidth))
				&& (y > rightArrowY && y < (rightArrowY + rightArrowHeight))) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(calendarDate);
			int prevMonth = calendar.get(Calendar.MONTH);

			int currentMonth = calendar.get(Calendar.MONTH);
			if (currentMonth == Calendar.DECEMBER) {
				calendar.set(Calendar.MONTH, Calendar.JANUARY);
				calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
			} else {
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
			}
			calendarDate = calendar.getTime();
			if (prevMonth == calendar.get(Calendar.MONTH)) {
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1);
				calendar.set(Calendar.DATE, 1);
				calendarDate = calendar.getTime();
			}
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