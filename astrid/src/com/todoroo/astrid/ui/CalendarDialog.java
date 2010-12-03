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

public class CalendarDialog extends Dialog implements OnClickListener {

	private final Button setButton;
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

		setButton = (Button) findViewById(R.id.SetButton);
		cancelButton = (Button) findViewById(R.id.CancelButton);

		calendarView = (CalendarView) findViewById(R.id.CalendarView);
		calendarView.setCalendarDate(calendarDate);

		setButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v == setButton) {
			calendarDate = calendarView.getCalendarDate();
			dismiss();
		} else if (v == cancelButton) {
			cancel();
		}
	}

	public Date getCalendarDate() {
		return calendarDate;
	}

	public void setCalendarDate(Date calendarDate) {
		this.calendarDate = calendarDate;
	}
}