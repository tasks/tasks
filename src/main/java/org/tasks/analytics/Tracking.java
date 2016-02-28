package org.tasks.analytics;

import org.tasks.R;

public class Tracking {

    public enum Events {
        SET_DEFAULT_LIST(R.string.tracking_category_preferences, R.string.tracking_action_set, R.string.p_default_list);

        public final int category;
        public final int action;
        public final int label;

        Events(int category, int action, int label) {
            this.category = category;
            this.action = action;
            this.label = label;
        }
    }
}
