/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import java.util.Date;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;

import com.timsu.astrid.R;
import com.todoroo.astrid.ui.CalendarView.OnSelectedDateListener;

public class CalendarDialog extends Dialog implements OnClickListener, OnSelectedDateListener {

	private final Button cancelButton;
	private Date calendarDate;

	private final CalendarView calendarView;

	public CalendarDialog(Context context, Date calendarDate) {
		super(context);
		this.calendarDate = calendarDate;
		/** 'Window.FEATURE_NO_TITLE' - Used to hide the title */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		/** Design the dialog in main.xml file */
		setContentView(R.layout.calendar);

		LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.FILL_PARENT;
        params.width = LayoutParams.FILL_PARENT;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

		cancelButton = (Button) findViewById(R.id.CancelButton);

		calendarView = (CalendarView) findViewById(R.id.CalendarView);
		calendarView.setCalendarDate(calendarDate);
		calendarView.setOnSelectedDateListener(this);

		cancelButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
	    if (v == cancelButton) {
			cancel();
		}
	}

	@Override
	public void onSelectedDate(Date date) {
	    calendarDate = date;
	    dismiss();
	}

	public Date getCalendarDate() {
		return calendarDate;
	}

	public void setCalendarDate(Date calendarDate) {
		this.calendarDate = calendarDate;
	}
}
