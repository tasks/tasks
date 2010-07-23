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

	LEVEL_1(0,
	        R.color.importance_1,
	        R.color.task_list_importance_1),
	LEVEL_2(0,
	        R.color.importance_2,
	        R.color.task_list_importance_2),
	LEVEL_3(0,
	        R.color.importance_3,
	        R.color.task_list_importance_3),
	LEVEL_4(0,
	        R.color.importance_4,
	        R.color.task_list_importance_4),

	// LEAST IMPORTANT
	;

	int label;
	int color;
	int taskListColor;
	public static final Importance DEFAULT = LEVEL_3;

	private Importance(int label, int color, int taskListColor) {
	    this.label = label;
	    this.color = color;
	    this.taskListColor = taskListColor;
	}

	public int getLabelResource() {
	    return label;
	}

	public int getColorResource() {
	    return color;
	}

	public int getTaskListColor() {
	    return taskListColor;
	}

}
