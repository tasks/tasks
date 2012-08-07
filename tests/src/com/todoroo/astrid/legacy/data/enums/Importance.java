/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.legacy.data.enums;


public enum Importance {
    // MOST IMPORTANT

	LEVEL_1(0,0,0),
	LEVEL_2(0,0,0),
	LEVEL_3(0,0,0),
	LEVEL_4(0,0,0)

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
