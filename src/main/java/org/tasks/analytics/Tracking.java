package org.tasks.analytics;

import org.tasks.R;

public class Tracking {

    public enum Events {
        SET_DEFAULT_LIST(R.string.tracking_category_preferences, R.string.p_default_list),
        SET_THEME(R.string.tracking_category_preferences, R.string.p_theme),
        WIDGET_ADD(R.string.tracking_category_widget, R.string.tracking_action_add),
        TIMER_START(R.string.tracking_category_timer, R.string.tracking_action_start);

        public final int category;
        public final int action;

        Events(int category, int action) {
            this.category = category;
            this.action = action;
        }
    }
}
