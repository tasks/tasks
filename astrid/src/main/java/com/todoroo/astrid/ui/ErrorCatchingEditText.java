/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class ErrorCatchingEditText extends EditText {

    public ErrorCatchingEditText(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);

    }

    public ErrorCatchingEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public ErrorCatchingEditText(Context context) {
        super(context);

    }

	@Override
	public int getExtendedPaddingTop() {
	    try {
            return super.getExtendedPaddingTop();
        } catch (Exception e) {
            return 0;
        }
	}

	@Override
	public int getExtendedPaddingBottom() {
	    try {
            return super.getExtendedPaddingBottom();
        } catch (Exception e) {
            return 0;
        }
	}

}
