package com.todoroo.astrid.alarms;

import android.content.Context;

import com.todoroo.andlib.service.DependencyInjectionService;

/**
 * Provides operations for working with alerts
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
@SuppressWarnings("nls")
public class AlarmService {

    /**
     * Metadata key for # of alarms
     */
    public static final String ALARM_COUNT = "alarms-count";

    public AlarmService(@SuppressWarnings("unused") Context context) {
        DependencyInjectionService.getInstance().inject(this);
    }

}
