package org.tasks.analytics;

import org.tasks.R;

public class Tracking {

    public enum Events {
        SET_DEFAULT_LIST(R.string.tracking_category_preferences, R.string.tracking_action_set, R.string.p_default_list),
        WIDGET_ADD_SCROLLABLE(R.string.tracking_category_widget, R.string.tracking_action_add, R.string.app_name),
        WIDGET_ADD_SHORTCUT(R.string.tracking_category_widget, R.string.tracking_action_add, R.string.FSA_label),
        TIMER_START(R.string.tracking_category_timer, R.string.tracking_action_start);

        public final int category;
        public final int action;
        public final int label;

        Events(int category, int action) {
            this(category, action, 0);
        }

        Events(int category, int action, int label) {
            this.category = category;
            this.action = action;
            this.label = label;
        }
    }
}
