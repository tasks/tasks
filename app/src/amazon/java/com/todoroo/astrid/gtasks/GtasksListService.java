package com.todoroo.astrid.gtasks;

import org.tasks.data.GoogleTaskList;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class GtasksListService {

    @Inject
    public GtasksListService() {

    }

    public List<GoogleTaskList> getLists() {
        return Collections.emptyList();
    }

    public GoogleTaskList getList(long storeId) {
        return null;
    }

    public GoogleTaskList getList(String remoteId) {
        return null;
    }
}
