/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;

import com.timsu.astrid.R;

public class NumberPickerDialog extends AlertDialog implements OnClickListener {

    public interface OnNumberPickedListener {
        void onNumberPicked(NumberPicker view, int number);
    }

    private final NumberPicker           mPicker;
    private final OnNumberPickedListener mCallback;

    public NumberPickerDialog(Context context, OnNumberPickedListener callBack,
            String title, int initialValue, int incrementBy, int start, int end) {
        super(context);
        mCallback = callBack;

        setButton(context.getText(android.R.string.ok), this);
        setButton2(context.getText(android.R.string.cancel), (OnClickListener) null);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.number_picker_dialog, null);
        setView(view);

        setTitle(title);
        mPicker = (NumberPicker) view.findViewById(R.id.numberPicker);
        mPicker.setIncrementBy(incrementBy);
        mPicker.setRange(start, end);
        mPicker.setCurrent(initialValue);
    }

    public void setInitialValue(int initialValue) {
        mPicker.setCurrent(initialValue);
    }

    public void onClick(DialogInterface dialog, int which) {
        if (mCallback != null) {
            mPicker.clearFocus();
            mCallback.onNumberPicked(mPicker, mPicker.getCurrent());
        }
    }
}
