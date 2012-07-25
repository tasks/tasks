/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
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
