package com.todoroo.astrid.alarms;

import android.content.Context;

import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.service.MetadataService;

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

    @Autowired
    private MetadataDao metadataDao;

    @Autowired
    private MetadataService metadataService;

    public AlarmService(@SuppressWarnings("unused") Context context) {
        DependencyInjectionService.getInstance().inject(this);
    }

}
