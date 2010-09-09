package com.todoroo.astrid.widget;

import com.timsu.astrid.R;

/**
 * Power Pack widget.  Supports 4x2 size.  Configured via
 * ConfigurePowerWidget42Activity when widget is added to homescreen.
 *
 * This class extends PowerWidget but sets references to itself for use in Intents.
 *
 * @author jwong (jwong@dayspring-tech.com)
 *
 */
@SuppressWarnings("nls")
public class PowerWidget42 extends PowerWidget {
    static final String LOG_TAG = "PowerWidget42";


    static {
        // set reference to my UpdateService for calls to launch the service
        updateService = PowerWidget42.UpdateService.class;

        ROW_LIMIT = 5;
    }

    /**
     * Extend PowerWidget's UpdateService so that the widget provider class
     * can be specified.  We can't just used PowerWidget's UpdateService
     * since it's a static class and uses PowerWidget's variables, not this
     * class' variables.
     *
     * @author jwong (jwong@dayspring-tech.com)
     *
     */
    public static class UpdateService extends PowerWidget.UpdateService {
        static {
            widgetClass = PowerWidget42.class;
            widgetLayout = R.layout.widget_power_42;
        }
    }

}
