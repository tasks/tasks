/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
