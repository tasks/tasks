package com.timsu.astrid.data.enums;

import com.timsu.astrid.R;

public enum Importance {
    // MOST IMPORTANT

	LEVEL_1(R.string.importance_1, R.color.importance_1),
	LEVEL_2(R.string.importance_2, R.color.importance_2),
	LEVEL_3(R.string.importance_3, R.color.importance_3),
	LEVEL_4(R.string.importance_4, R.color.importance_4),

	// LEAST IMPORTANT
	;

	int label;
	int color;
	public static final Importance DEFAULT = LEVEL_2;

	private Importance(int label, int color) {
	    this.label = label;
	    this.color = color;
	}

	public int getLabelResource() {
	    return label;
	}

	public int getColorResource() {
	    return color;
	}
}
