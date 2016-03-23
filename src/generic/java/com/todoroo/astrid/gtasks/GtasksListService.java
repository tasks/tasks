package com.todoroo.astrid.gtasks;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class GtasksListService {

    @Inject
    public GtasksListService() {

    }

    public List<GtasksList> getLists() {
        return Collections.emptyList();
    }

    public GtasksList getList(long storeId) {
        return null;
    }
}
