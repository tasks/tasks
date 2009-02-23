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
package com.timsu.astrid.data.enums;

import java.util.Date;

import android.content.res.Resources;

import com.timsu.astrid.R;

public enum RepeatInterval {

	DAYS(R.string.repeat_days) {
        @Override
        public void offsetDateBy(Date input, int number) {
            input.setDate(input.getDate() + number);
        }
	},
	WEEKS(R.string.repeat_weeks) {
        @Override
        public void offsetDateBy(Date input, int number) {
            input.setDate(input.getDate() + 7 * number);
        }
    },
	MONTHS(R.string.repeat_months) {
        @Override
        public void offsetDateBy(Date input, int number) {
            input.setMonth(input.getMonth() + number);
        }
    },
    HOURS(R.string.repeat_hours) {
        @Override
        public void offsetDateBy(Date input, int number) {
            input.setHours(input.getHours() + number);
        }
    },

	;

    int label;

    private RepeatInterval(int label) {
        this.label = label;
    }

    public int getLabelResource() {
        return label;
    }

	abstract public void offsetDateBy(Date input, int number);

	public static String[] getLabels(Resources r) {
	    int intervalCount = RepeatInterval.values().length;
	    String[] result = new String[intervalCount];

	    for(int i = 0; i < intervalCount; i++)
	        result[i] = r.getString(RepeatInterval.values()[i].getLabelResource());
	    return result;
	}
}
