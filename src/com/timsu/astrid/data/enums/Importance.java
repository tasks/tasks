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

import com.timsu.astrid.R;

public enum Importance {
    // MOST IMPORTANT

	LEVEL_1(R.string.importance_1, R.color.importance_1, R.drawable.bullet_red),
	LEVEL_2(R.string.importance_2, R.color.importance_2, R.drawable.bullet_orange),
	LEVEL_3(R.string.importance_3, R.color.importance_3, R.drawable.bullet_blue),
	LEVEL_4(R.string.importance_4, R.color.importance_4, R.drawable.bullet_white),

	// LEAST IMPORTANT
	;

	int label;
	int color;
	int icon;
	public static final Importance DEFAULT = LEVEL_3;

	private Importance(int label, int color, int icon) {
	    this.label = label;
	    this.color = color;
	    this.icon = icon;
	}

	public int getLabelResource() {
	    return label;
	}

	public int getColorResource() {
	    return color;
	}

	public int getIconResource() {
	    return icon;
	}
}
